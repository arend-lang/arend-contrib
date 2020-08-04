package org.arend.contrib.meta;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.TypedExpression;

import java.util.List;

public class LaterMeta extends MetaInvocationMeta {
  @Override
  public boolean requireExpectedType() {
    return true;
  }

  @Override
  public TypedExpression invokeMeta(MetaDefinition meta, List<ConcreteExpression> implicitArgs, ExpressionTypechecker typechecker, ContextData contextData) {
    return typechecker.defer(meta, contextData, contextData.getExpectedType());
  }
}
