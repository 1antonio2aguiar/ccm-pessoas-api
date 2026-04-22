package br.com.cc.pessoas.entity.enuns;

public enum TipoEndereco {
    CASA(0,"CASA"),
    TRABALHO(1,"TRABALHO");
    private final Integer codigo;
    private final String descricao;

    private TipoEndereco(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }
    public String getDescricao() { return descricao; }

    // static pode chamar se que o obj seja instanciado.
    public static TipoEndereco toTipoEnderecoEnum(Integer codigo) {
        if(codigo == null) return null;

        for (TipoEndereco tp : TipoEndereco.values()) {
            if(codigo.equals(tp.getCodigo())) {
                return tp;
            }
        }
        throw new IllegalArgumentException("Tipo endereço inválido " + codigo);
    }
}
