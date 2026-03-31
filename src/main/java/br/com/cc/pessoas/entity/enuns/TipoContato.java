package br.com.cc.pessoas.entity.enuns;

public enum TipoContato {
    TELEFONE_FIXO(0,"TELEFONE FIXO"),
    TELEFONE_CELULAR(1,"TELEFONE CELULAR"),
    WHATSUP(2,"WHATSUP"),
    E_MAIL(3,"E_MAIL"),
    PAGINA_WEB(4,"PAGINA WEB"),
    TELEFONE_PARA_RECADO(5,"TELEFONE PARA RECADO");
    private final Integer codigo;
    private final String descricao;

    private TipoContato(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }
    public String getDescricao() { return descricao; }

    // static pode chamar se que o obj seja instanciado.
    public static TipoContato toTipoContatoEnum(Integer codigo) {
        if(codigo == null) return null;

        for (TipoContato tp : TipoContato.values()) {
            if(codigo.equals(tp.getCodigo())) {
                return tp;
            }
        }
        throw new IllegalArgumentException("Tipo contato inválido " + codigo);
    }
}
