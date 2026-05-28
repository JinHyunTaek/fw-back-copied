package my.mma.api.fighter.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Country {

    // 아메리카
    US("미국"),
    CA("캐나다"),
    MX("멕시코"),
    BR("브라질"),
    AR("아르헨티나"),
    CO("콜롬비아"),
    VE("베네수엘라"),
    CL("칠레"),
    PE("페루"),
    EC("에콰도르"),
    PA("파나마"),
    DO("도미니카 공화국"),
    PR("푸에르토리코"),
    CU("쿠바"),
    JM("자메이카"),
    TT("트리니다드 토바고"),
    SR("수리남"),

    // 유럽 (영국 본토)
    GB_ENG("잉글랜드"),
    GB_SCT("스코틀랜드"),
    GB_WLS("웨일스"),
    GB_NIR("북아일랜드"),
    IE("아일랜드"),
    FR("프랑스"),
    DE("독일"),
    NL("네덜란드"),
    BE("벨기에"),
    CH("스위스"),
    AT("오스트리아"),
    IT("이탈리아"),
    ES("스페인"),
    PT("포르투갈"),
    SE("스웨덴"),
    NO("노르웨이"),
    DK("덴마크"),
    FI("핀란드"),
    PL("폴란드"),
    CZ("체코"),
    SK("슬로바키아"),
    HU("헝가리"),
    RO("루마니아"),
    BG("불가리아"),
    RS("세르비아"),
    HR("크로아티아"),
    MK("북마케도니아"),
    AL("알바니아"),
    LT("리투아니아"),
    LV("라트비아"),
    EE("에스토니아"),
    MD("몰도바"),
    UA("우크라이나"),
    BY("벨라루스"),
    RU("러시아"),
    GE("조지아"),
    AM("아르메니아"),
    AZ("아제르바이잔"),

    // 중앙아시아
    KZ("카자흐스탄"),
    KG("키르기스스탄"),
    UZ("우즈베키스탄"),
    TJ("타지키스탄"),

    // 아시아
    KR("한국"),
    JP("일본"),
    CN("중국"),
    IR("이란"),
    TR("터키"),
    PH("필리핀"),
    TH("태국"),
    ID("인도네시아"),
    SG("싱가포르"),

    // 오세아니아
    AU("호주"),
    NZ("뉴질랜드"),

    // 아프리카
    NG("나이지리아"),
    CM("카메룬"),
    GH("가나"),
    SN("세네갈"),
    MA("모로코"),
    EG("이집트"),
    AO("앙골라"),
    ZA("남아프리카공화국");

    private final String description;

}
