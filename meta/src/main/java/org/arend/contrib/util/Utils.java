package org.arend.contrib.util;

import org.arend.contrib.StdExtension;
import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.*;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.expr.*;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.TypeMismatchError;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.instance.InstanceSearchParameters;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.MetaRef;
import org.arend.ext.typechecking.BaseMetaDefinition;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.TypedExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Utils {
  public static int getNumber(ConcreteExpression expression, ErrorReporter errorReporter) {
    if (expression instanceof ConcreteNumberExpression) {
      return ((ConcreteNumberExpression) expression).getNumber().intValue();
    } else {
      if (errorReporter != null) {
        errorReporter.report(new TypecheckingError("Expected a number", expression));
      }
      return -1;
    }
  }

  public static List<? extends ConcreteExpression> getArgumentList(ConcreteExpression expr) {
    return expr instanceof ConcreteTupleExpression ? ((ConcreteTupleExpression) expr).getFields() : Collections.singletonList(expr);
  }

  public static CoreFunCallExpression toEquality(CoreExpression expression, ErrorReporter errorReporter, ConcreteSourceNode sourceNode) {
    CoreFunCallExpression equality = expression.toEquality();
    if (equality == null && errorReporter != null) {
      errorReporter.report(new TypeMismatchError(DocFactory.text("_ = _"), expression, sourceNode));
    }
    return equality;
  }

  public static CoreExpression getAppArguments(CoreExpression expression, int numberOfArgs, List<CoreExpression> args) {
    CoreExpression result = getAppArgumentsRev(expression, numberOfArgs, args);
    Collections.reverse(args);
    return result;
  }

  private static CoreExpression getAppArgumentsRev(CoreExpression expression, int numberOfArgs, List<CoreExpression> args) {
    if (numberOfArgs <= 0) {
      return expression;
    }
    expression = expression.normalize(NormalizationMode.WHNF).getUnderlyingExpression();
    if (expression instanceof CoreAppExpression) {
      args.add(((CoreAppExpression) expression).getArgument());
      return getAppArgumentsRev(((CoreAppExpression) expression).getFunction(), numberOfArgs - 1, args);
    }
    return expression;
  }

  public static int numberOfExplicitParameters(CoreDefinition def) {
    int result = def instanceof CoreFunctionDefinition ? numberOfExplicitPiParameters(((CoreFunctionDefinition) def).getResultType())
      : def instanceof CoreClassField ? numberOfExplicitPiParameters(((CoreClassField) def).getResultType()) : 0;
    for (CoreParameter parameter = def.getParameters(); parameter.hasNext(); parameter = parameter.getNext()) {
      if (parameter.isExplicit()) {
        result++;
      }
    }
    return result;
  }

  public static int numberOfExplicitPiParameters(CoreExpression type) {
    List<CoreParameter> parameters = new ArrayList<>();
    type.getPiParameters(parameters);

    int result = 0;
    for (CoreParameter parameter : parameters) {
      if (parameter.isExplicit()) {
        result++;
      }
    }
    return result;
  }

  public static ConcreteExpression addArguments(ConcreteExpression expr, ConcreteFactory factory, int numberOfArgs, boolean addGoals) {
    if (numberOfArgs <= 0) {
      return expr;
    }

    List<ConcreteExpression> args = new ArrayList<>(numberOfArgs);
    for (int i = 0; i < numberOfArgs; i++) {
      args.add(addGoals ? factory.goal() : factory.hole());
    }
    return factory.app(expr, true, args);
  }

  public static ConcreteExpression addArguments(ConcreteExpression expr, StdExtension ext, int expectedParameters, boolean addGoals) {
    ConcreteReferenceExpression refExpr = null;
    if (expr instanceof ConcreteReferenceExpression) {
      refExpr = (ConcreteReferenceExpression) expr;
    } else if (expr instanceof ConcreteAppExpression) {
      ConcreteExpression fun = ((ConcreteAppExpression) expr).getFunction();
      if (fun instanceof ConcreteReferenceExpression) {
        refExpr = (ConcreteReferenceExpression) fun;
      }
    }

    if (refExpr == null) {
      return expr;
    }

    int numberOfArgs = 0;
    ArendRef ref = refExpr.getReferent();
    if (ref instanceof MetaRef) {
      MetaDefinition meta = ((MetaRef) ref).getDefinition();
      if (meta instanceof BaseMetaDefinition) {
        boolean[] explicitness = ((BaseMetaDefinition) meta).argumentExplicitness();
        if (explicitness != null) {
          for (boolean explicit : explicitness) {
            if (explicit) {
              numberOfArgs++;
            }
          }
        }
      }
    } else {
      CoreDefinition argDef = ext.definitionProvider.getCoreDefinition(ref);
      if (argDef == null) {
        return expr;
      }
      numberOfArgs = numberOfExplicitParameters(argDef) - expectedParameters;
    }

    if (expr instanceof ConcreteAppExpression && numberOfArgs > 0) {
      for (ConcreteArgument argument : ((ConcreteAppExpression) expr).getArguments()) {
        if (argument.isExplicit()) {
          numberOfArgs--;
        }
      }
    }

    return addArguments(expr, ext.factory.withData(expr.getData()), numberOfArgs, addGoals);
  }

  public static TypedExpression typecheckWithAdditionalArguments(ConcreteExpression expr, ExpressionTypechecker typechecker, StdExtension ext, int expectedParameters, boolean addGoals) {
    TypedExpression result = typechecker.typecheck(addArguments(expr, ext, expectedParameters, addGoals), null);
    if (result == null) {
      return null;
    }

    List<CoreParameter> parameters = new ArrayList<>();
    result.getType().getPiParameters(parameters);
    if (parameters.isEmpty()) {
      return result;
    }

    ConcreteFactory factory = ext.factory.withData(expr.getData());
    List<ConcreteArgument> arguments = new ArrayList<>();
    for (CoreParameter parameter : parameters) {
      if (parameter.isExplicit()) {
        if (expectedParameters <= 0) {
          arguments.add(factory.arg(addGoals ? factory.goal() : factory.hole(), true));
        } else {
          expectedParameters--;
        }
      } else {
        arguments.add(factory.arg(factory.hole(), false));
      }
    }
    if (arguments.isEmpty()) {
      return result;
    }

    return typechecker.typecheck(factory.app(factory.core("_", result), arguments), null);
  }

  private static class MyException extends RuntimeException {}

  public static <T> T tryTypecheck(ExpressionTypechecker typechecker, Function<ExpressionTypechecker, T> action) {
    try {
      return typechecker.withErrorReporter(error -> {
        if (error.level == GeneralError.Level.ERROR) {
          throw new MyException();
        }
      }, action);
    } catch (MyException e) {
      return null;
    }
  }

  public static TypedExpression findInstance(InstanceSearchParameters parameters, CoreClassField classifyingField, CoreExpression classifyingExpr, ExpressionTypechecker typechecker, ConcreteSourceNode marker) {
    CoreExpression expr = classifyingExpr.normalize(NormalizationMode.WHNF);
    if (expr instanceof CoreFieldCallExpression && ((CoreFieldCallExpression) expr).getDefinition() == classifyingField) {
      TypedExpression result = ((CoreFieldCallExpression) expr).getArgument().computeTyped();
      CoreExpression type = result.getType().normalize(NormalizationMode.WHNF);
      return type instanceof CoreClassCallExpression && parameters.testClass(((CoreClassCallExpression) type).getDefinition()) ? result : null;
    } else {
      return typechecker.findInstance(parameters, expr, null, marker);
    }
  }
}
