package my.mma.api.fightevent.entity.property;

import lombok.Getter;

@Getter
public enum WinMethod {

    SUB,
    KO_TKO,
    // for user
    DEC,
    // for ffe
    U_DEC,
    // for ffe
    M_DEC,
    // for ffe
    S_DEC,
    DQ,

}
