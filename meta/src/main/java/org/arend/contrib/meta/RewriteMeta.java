package org.arend.contrib.meta;

import org.arend.contrib.StdExtension;
import org.arend.contrib.error.SubexprError;
import org.arend.contrib.util.Utils;
import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.expr.CoreFunCallExpression;
import org.arend.ext.core.expr.CoreInferenceReferenceExpression;
import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.typechecking.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RewriteMeta extends BaseMetaDefinition {
  private final StdExtension ext;
  private final boolean isForward;
  private final boolean isInverse;

  public RewriteMeta(StdExtension ext, boolean isForward, boolean isInverse) {
    this.ext = ext;
    this.isForward = isForward;
    this.isInverse = isInverse;
  }

  @Override
  public boolean withoutLevels() {
    return false;
  }

  @Override
  public boolean[] argumentExplicitness() {
    return new boolean[] { false, true, true };
  }

  private void getNumber(ConcreteExpression expression, Set<Integer> result, ErrorReporter errorReporter) {
    int n = Utils.getNumber(expression, errorReporter);
    if (n >= 0) {
      result.add(n);
    }
  }

  @Override
  public @Nullable ConcreteExpression getConcreteRepresentation(@NotNull List<? extends ConcreteArgument> arguments) {
    return ext.factory.appBuilder(ext.factory.ref(ext.transportInv.getRef())).app(ext.factory.hole()).app(arguments.subList(arguments.get(0).isExplicit() ? 0 : 1, arguments.size())).build();
  }

  @Override
  public TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    ErrorReporter errorReporter = typechecker.getErrorReporter();
    ConcreteReferenceExpression refExpr = contextData.getReferenceExpression();
    ConcreteFactory factory = ext.factory.withData(refExpr.getData());
    List<? extends ConcreteArgument> args = contextData.getArguments();
    int currentArg = 0;

    // Collect occurrences
    Set<Integer> occurrences;
    if (!args.get(0).isExplicit()) {
      occurrences = new HashSet<>();
      for (ConcreteExpression expr : Utils.getArgumentList(args.get(0).getExpression())) {
        getNumber(expr, occurrences, errorReporter);
      }
      currentArg++;
    } else {
      occurrences = null;
    }

    CoreExpression expectedType = contextData.getExpectedType() == null ? null : contextData.getExpectedType().getUnderlyingExpression();
    boolean isForward = this.isForward || expectedType == null || args.size() > currentArg + 2;

    // Add inference holes to functions and type-check the path argument
    ConcreteExpression arg0 = args.get(currentArg++).getExpression();
    TypedExpression path = Utils.typecheckWithAdditionalArguments(arg0, typechecker, ext, 0, false);
    if (path == null) {
      return null;
    }

    // Check that the first argument is a path
    CoreFunCallExpression eq = Utils.toEquality(path.getType(), errorReporter, arg0);
    if (eq == null) {
      return null;
    }

    ConcreteExpression transport = factory.ref((isInverse ? ext.transportInv : ext.transport).getRef(), refExpr.getPLevel(), refExpr.getHLevel());
    CoreExpression value = eq.getDefCallArguments().get(isInverse == isForward ? 2 : 1);

    // This case won't happen often, but sill possible
    if (!isForward && expectedType instanceof CoreInferenceReferenceExpression) {
      CoreExpression var = value.getUnderlyingExpression();
      if (var instanceof CoreInferenceReferenceExpression && ((CoreInferenceReferenceExpression) var).getVariable() == ((CoreInferenceReferenceExpression) expectedType).getVariable()) {
        if (!(occurrences == null || occurrences.isEmpty() || occurrences.size() == 1 && occurrences.contains(1))) {
          occurrences.remove(1);
          errorReporter.report(new SubexprError(occurrences, var, expectedType, refExpr));
          return null;
        }
        ArendRef ref = factory.local("T");
        return typechecker.typecheck(factory.app(transport, true, Arrays.asList(
          factory.lam(Collections.singletonList(factory.param(ref)), factory.ref(ref)),
          factory.core("transport (\\lam T => T) {!} _", path),
          args.get(currentArg).getExpression())), null);
      }
      isForward = true;
    }

    TypedExpression lastArg;
    CoreExpression type;
    if (isForward) {
      lastArg = typechecker.typecheck(args.get(currentArg++).getExpression(), null);
      if (lastArg == null) {
        return null;
      }
      type = lastArg.getType();
    } else {
      lastArg = null;
      type = expectedType;
    }

    // Replace occurrences and return the result
    ArendRef ref = factory.local("x");
    return typechecker.typecheck(factory.appBuilder(transport)
      .app(factory.lam(Collections.singletonList(factory.param(ref)), factory.meta("transport (\\lam x => {!}) _ _", new MetaDefinition() {
        @Override
        public TypedExpression invokeMeta(@NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
          TypedExpression var = typechecker.typecheck(factory.ref(ref), null);
          assert var != null;
          final int[] num = { 0 };
          UncheckedExpression absExpr = type.replaceSubexpressions(expression -> {
            if (typechecker.compare(expression, value, CMP.EQ, refExpr, false, true)) {
              num[0]++;
              if (occurrences == null || occurrences.contains(num[0])) {
                return var.getExpression();
              }
            }
            return null;
          });
          if (absExpr == null) {
            errorReporter.report(new TypecheckingError("Cannot substitute expression", refExpr));
            return null;
          }
          if (occurrences != null && num[0] > 0) {
            occurrences.removeIf(i -> i <= num[0]);
          }
          if (num[0] == 0 || occurrences != null && !occurrences.isEmpty()) {
            errorReporter.report(new SubexprError(occurrences, value, type, refExpr));
            return null;
          }
          return typechecker.check(absExpr, refExpr);
        }
      })))
      .app(factory.core("transport _ {!} _", path))
      .app(lastArg == null ? args.get(currentArg++).getExpression() : factory.core("transport _ _ {!}", lastArg))
      .app(args.subList(currentArg, args.size()))
      .build(), null);
  }
}
