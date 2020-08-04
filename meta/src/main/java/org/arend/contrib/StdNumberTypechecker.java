package org.arend.contrib;

import org.arend.ext.NumberTypechecker;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.typechecking.ContextData;
import org.arend.ext.typechecking.ExpressionTypechecker;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public class StdNumberTypechecker implements NumberTypechecker {
  private final StdExtension ext;

  public StdNumberTypechecker(StdExtension ext) {
    this.ext = ext;
  }

  @Override
  public @Nullable TypedExpression typecheckNumber(@NotNull BigInteger number, @NotNull ExpressionTypechecker typechecker, @NotNull ContextData contextData) {
    CoreExpression expectedType = contextData.getExpectedType() == null ? null : contextData.getExpectedType().normalize(NormalizationMode.WHNF);
    return typechecker.checkNumber(number, expectedType, contextData.getMarker());
  }
}
