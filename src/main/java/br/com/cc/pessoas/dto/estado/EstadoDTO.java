package br.com.cc.pessoas.dto.estado;

import br.com.cc.pessoas.entity.Estado;

public record EstadoDTO(
        Long id,
        Long paisId,
        String paisNome,
        String nome,
        String uf,
        Long codigoInep
) {

    public static EstadoDTO fromEstado(Estado estado) {
        if (estado == null) return null;

        return new EstadoDTO(
                estado.getId(),
                estado.getPais().getId(),
                estado.getPais().getNome(),
                estado.getNome(),
                estado.getUf(),
                estado.getCodigoInep()
        );
    }
}
