package br.com.cc.pessoas.entity.enuns;

public enum TipoDocumento {
    CTPS(0,"CTPS"),
    CNH(1,"CNH"),
    INSCRIÇÃO_MUNICIPAL(2,"INSCRIÇÃO MUNICIPAL"),
    PASSAPORTE(3,"PASSAPORTE"),
    RESERVISTA(4,"RESERVISTA"),
    RG(5,"RG"),
    TÍTULOD_DE_ELEITOR(6,"TÍTULO DE ELEITOR"),
    RG_ESTRANGEIRO(7,"RG PARA ESTRANGEIRO"),
    CREA(8,"CREA"),
    CRM(9,"CRM");
    private final Integer codigo;
    private final String descricao;

    private TipoDocumento(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }
    public String getDescricao() { return descricao; }

    // static pode chamar se que o obj seja instanciado.
    public static TipoDocumento toTipoDocumentoEnum(Integer codigo) {
        if(codigo == null) return null;

        for (TipoDocumento tp : TipoDocumento.values()) {
            if(codigo.equals(tp.getCodigo())) {
                return tp;
            }
        }
        throw new IllegalArgumentException("Tipo documento inválido " + codigo);
    }
}
