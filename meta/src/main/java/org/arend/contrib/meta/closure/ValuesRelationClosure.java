package org.arend.contrib.meta.closure;

import org.arend.contrib.util.Values;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.expr.UncheckedExpression;

public class ValuesRelationClosure implements BinaryRelationClosure<UncheckedExpression> {
  public final Values<UncheckedExpression> values;
  public final BinaryRelationClosure<Integer> closure;

  public ValuesRelationClosure(Values<UncheckedExpression> values, BinaryRelationClosure<Integer> closure) {
    this.values = values;
    this.closure = closure;
  }

  @Override
  public void addRelation(UncheckedExpression value1, UncheckedExpression value2, ConcreteExpression proof) {
    closure.addRelation(values.addValue(value1), values.addValue(value2), proof);
  }

  @Override
  public ConcreteExpression checkRelation(UncheckedExpression value1, UncheckedExpression value2) {
    return closure.checkRelation(values.addValue(value1), values.addValue(value2));
  }
}
