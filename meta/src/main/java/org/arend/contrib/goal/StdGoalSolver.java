package org.arend.contrib.goal;

import org.arend.contrib.StdExtension;
import org.arend.contrib.util.Utils;
import org.arend.ext.concrete.expr.ConcreteAppExpression;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.concrete.expr.ConcreteGoalExpression;
import org.arend.ext.concrete.expr.ConcreteReferenceExpression;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.GoalSolver;
import org.arend.ext.typechecking.InteractiveGoalSolver;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class StdGoalSolver implements GoalSolver {
  private final StdExtension ext;

  public StdGoalSolver(StdExtension ext) {
    this.ext = ext;
  }

  @Override
  public @NotNull CheckGoalResult checkGoal(@NotNull ExpressionTypechecker typechecker, @NotNull ConcreteGoalExpression goalExpression, @Nullable CoreExpression expectedType) {
    ConcreteExpression expr = goalExpression.getExpression();
    if (expr == null) {
      return new CheckGoalResult(null, null);
    }

    if (expectedType == null || !(expr instanceof ConcreteReferenceExpression || expr instanceof ConcreteAppExpression)) {
      return new CheckGoalResult(expr, typechecker.typecheck(expr, expectedType));
    }

    int expectedParams = Utils.numberOfExplicitPiParameters(expectedType);

    Object exprData = expr.getData();
    ErrorReporter errorReporter = typechecker.getErrorReporter();
    ConcreteExpression extExpr = Utils.addArguments(expr, ext, expectedParams, true);
    TypedExpression result = typechecker.withErrorReporter(error -> {
      if (!(error.level == GeneralError.Level.GOAL && error.getCause() == exprData)) {
        errorReporter.report(error);
      }
    }, tc -> tc.typecheck(extExpr, expectedType));

    return new CheckGoalResult(result == null ? extExpr : Utils.addArguments(extExpr, ext.factory.withData(exprData), Utils.numberOfExplicitPiParameters(result.getType()) - expectedParams, true), result);
  }

  @Override
  public @NotNull Collection<? extends InteractiveGoalSolver> getAdditionalSolvers() {
    return Collections.singletonList(new ConstructorGoalSolver(ext));
  }
}
