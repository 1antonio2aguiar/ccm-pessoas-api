package br.com.cc.pessoas.unificacao.controle.ctrlService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class ControleMigracaoPessoaService {

    private static final String STATUS_NAO_MIGRAR = "N";

    private static final Set<String> BANCOS_PERMITIDOS =
            Set.of(
                    "PESSOAS",
                    "RH",
                    "SANE"
            );

    @PersistenceContext
    private EntityManager manager;

    /*
     * Registra uma única pessoa como não autorizada
     * para unificação.
     */
    public void registrarNaoMigrar(
            String banco,
            Long cdOrigem) {

        registrarNaoMigrar(
                banco,
                cdOrigem,
                null,
                null
        );
    }

    /*
     * Registra uma única pessoa, permitindo informar
     * usuário e motivo futuramente.
     */
    public void registrarNaoMigrar(
            String banco,
            Long cdOrigem,
            String usuario,
            String motivo) {

        String bancoNormalizado =
                normalizarBanco(banco);

        validarCdOrigem(cdOrigem);

        manager.createNativeQuery("""
            merge into DBO_CCM_PESSOAS.CONTROLE_MIGRACAO_PESSOA destino
            using
            (
                select
                    :banco as banco,
                    :cdOrigem as cd_origem
                  from dual
            ) origem
               on
               (
                   destino.BANCO = origem.banco
                   and destino.CD_ORIGEM = origem.cd_origem
               )

            when matched then
                update set
                    destino.STATUS_MIGRACAO = :statusMigracao,
                    destino.DATA_REGISTRO = sysdate,
                    destino.USUARIO = :usuario,
                    destino.MOTIVO = :motivo

            when not matched then
                insert
                (
                    ID,
                    BANCO,
                    CD_ORIGEM,
                    STATUS_MIGRACAO,
                    DATA_REGISTRO,
                    USUARIO,
                    MOTIVO
                )
                values
                (
                    DBO_CCM_PESSOAS.SEQ_CONTROLE_MIGRACAO_PESSOA.nextval,
                    :banco,
                    :cdOrigem,
                    :statusMigracao,
                    sysdate,
                    :usuario,
                    :motivo
                )
        """)
                .setParameter(
                        "banco",
                        bancoNormalizado
                )
                .setParameter(
                        "cdOrigem",
                        cdOrigem
                )
                .setParameter(
                        "statusMigracao",
                        STATUS_NAO_MIGRAR
                )
                .setParameter(
                        "usuario",
                        usuario
                )
                .setParameter(
                        "motivo",
                        motivo
                )
                .executeUpdate();
    }

    /*
     * Utilizado no CPF duplicado.
     *
     * Todos os integrantes do grupo são registrados
     * dentro da mesma transação.
     */
    public void registrarGrupoNaoMigrar(
            String banco,
            Collection<Long> codigosOrigem) {

        if (codigosOrigem == null
                || codigosOrigem.isEmpty()) {

            throw new IllegalArgumentException(
                    "O grupo de pessoas não foi informado."
            );
        }

        for (Long cdOrigem : codigosOrigem) {

            registrarNaoMigrar(
                    banco,
                    cdOrigem
            );
        }
    }

    private String normalizarBanco(
            String banco) {

        if (banco == null
                || banco.isBlank()) {

            throw new IllegalArgumentException(
                    "O banco de origem não foi informado."
            );
        }

        String bancoNormalizado =
                banco.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (!BANCOS_PERMITIDOS.contains(
                bancoNormalizado
        )) {

            throw new IllegalArgumentException(
                    "Banco de origem inválido: "
                            + bancoNormalizado + "."
            );
        }

        return bancoNormalizado;
    }

    private void validarCdOrigem(
            Long cdOrigem) {

        if (cdOrigem == null
                || cdOrigem <= 0L) {

            throw new IllegalArgumentException(
                    "O código de origem da pessoa é inválido."
            );
        }
    }
}