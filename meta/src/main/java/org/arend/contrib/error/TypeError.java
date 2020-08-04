package org.arend.contrib.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.jetbrains.annotations.Nullable;

public class TypeError extends TypecheckingError {
  public final CoreExpression type;

  public TypeError(String message, CoreExpression type, @Nullable ConcreteSourceNode cause) {
    super(message, cause);
    this.type = type;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return DocFactory.hang(DocFactory.text("Type:"), DocFactory.termDoc(type, ppConfig));
  }
}
