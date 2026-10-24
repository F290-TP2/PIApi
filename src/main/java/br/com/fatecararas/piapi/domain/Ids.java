package br.com.fatecararas.piapi.domain;

import org.bson.types.ObjectId;

/** Mantém o mesmo formato canônico para IDs e referências manuais. */
public final class Ids {
    private Ids() { }

    public static String normalizar(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw new RequisicaoInvalidaException("Identificador deve conter 24 caracteres hexadecimais.");
        }
        return new ObjectId(id).toHexString();
    }
}
