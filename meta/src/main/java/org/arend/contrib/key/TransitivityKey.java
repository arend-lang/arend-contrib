package org.arend.contrib.key;

import org.arend.contrib.StdExtension;
import org.arend.ext.core.context.CoreParameter;
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.expr.CoreExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TransitivityKey extends FieldKey {
  public TransitivityKey(@NotNull String name, StdExtension ext) {
    super(name, ext);
  }

  @Override
  protected int getNumberOfParameters() {
    return 5;
  }

  @Override
  protected boolean checkField(CoreClassField field) {
    List<CoreParameter> parameters = new ArrayList<>();
    CoreExpression codomain = field.getResultType().getPiParameters(parameters);
    if (!(parameters.size() == 5 && isBaseSetCall() && isBaseSetCall() && isBaseSetCall())) {
      return false;
    }

    CoreClassField relation = getFieldApplied(parameters.get(3).getTypeExpr(), parameters.get(0).getBinding(), parameters.get(1).getBinding(), field);
    if (relation == null) {
      return false;
    }

    if (relation != getFieldApplied(parameters.get(4).getTypeExpr(), parameters.get(1).getBinding(), parameters.get(2).getBinding(), field) || relation != getFieldApplied(codomain, parameters.get(0).getBinding(), parameters.get(2).getBinding(), field)) {
      return false;
    }

    putData(relation, field, parameters);
    return true;
  }
}
