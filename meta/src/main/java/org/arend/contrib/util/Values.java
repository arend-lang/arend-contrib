package org.arend.contrib.util;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.typechecking.ExpressionTypechecker;

import java.util.ArrayList;
import java.util.List;

public class Values<E extends UncheckedExpression> {
  private final ExpressionTypechecker typechecker;
  private final ConcreteSourceNode marker;
  private final List<E> values = new ArrayList<>();

  public Values(ExpressionTypechecker typechecker, ConcreteSourceNode marker) {
    this.typechecker = typechecker;
    this.marker = marker;
  }

  public int addValue(E value) {
    int index = getIndex(value);
    if (index == -1) {
      values.add(value);
      return values.size() - 1;
    } else {
      return index;
    }
  }

  public int getIndex(UncheckedExpression value) {
    for (int i = 0; i < values.size(); i++) {
      if (typechecker != null ? typechecker.compare(value, values.get(i), CMP.EQ, marker, false, true) : value.compare(values.get(i), CMP.EQ)) {
        return i;
      }
    }
    return -1;
  }

  public E getValue(int index) {
    return values.get(index);
  }

  public List<E> getValues() {
    return values;
  }
}
