package br.com.cc.pessoas.entity.enuns;

public enum EstadoCivil {
    AMAZIADO(0,"Amaziado(a)"),
    CASADO(1,"Casado(a)"),
    DIVORCIADO(2,"Divorciado(a)"),
    SOLTEIRO(3,"Solteiro(a)"),
    UNIAO_ESTAVEL(4,"União estável"),
    VIUVO(5,"Viúvo(a)"),
    OUTRO(6,"Outro");
    private final Integer codigo;
    private final String descricao;

    private EstadoCivil(Integer codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public Integer getCodigo() {
        return codigo;
    }
    public String getDescricao() { return descricao; }

    // static pode chamar se que o obj seja instanciado.
    public static EstadoCivil toEstadoCivilEnum(Integer codigo) {
        if(codigo == null) return null;

        for (EstadoCivil tp : EstadoCivil.values()) {
            if(codigo.equals(tp.getCodigo())) {
                return tp;
            }
        }
        throw new IllegalArgumentException("Tipo contato inválido " + codigo);
    }
}
