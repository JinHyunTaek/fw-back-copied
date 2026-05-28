package my.mma.api.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.alert.entity.UserPreferences;
import my.mma.api.alert.repository.UserPreferencesRepository;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.faq.entity.FAQ;
import my.mma.api.faq.entity.FAQCategory;
import my.mma.api.faq.repository.FAQRepository;
import my.mma.api.fighter.entity.Country;
import my.mma.api.fighter.entity.FightRecord;
import my.mma.api.fighter.entity.Fighter;
import my.mma.api.fighter.repository.FighterRepository;
import my.mma.api.fightevent.entity.FightEvent;
import my.mma.api.fightevent.entity.FighterFightEvent;
import my.mma.api.fightevent.entity.property.FightResult;
import my.mma.api.fightevent.entity.property.FightWeight;
import my.mma.api.fightevent.entity.property.WinMethod;
import my.mma.api.fightevent.repository.FightEventRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static my.mma.api.global.utils.CustomDateUtils.getFightDuration;
import static my.mma.api.global.utils.CustomUnitUtils.toCentimeter;
import static my.mma.api.global.utils.ModifyUtils.toKg;

@Slf4j
//@Component
@Profile("dev")
@RequiredArgsConstructor
public class InitializeFightersAndEvents {

    private final FighterRepository fighterRepository;
    private final FightEventRepository fightEventRepository;
    private final UserRepository userRepository;
    private final FAQRepository faqRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserPreferencesRepository userPreferencesRepository;

    @Value("${ufc_json_path}")
    private String ufcDataJsonPath;

    @Value("${faq_json_path}")
    private String seedFaqJsonPath;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPwd;

    @Value("${admin.nickname}")
    private String adminNickname;

    /**
     * 스프링 애플리케이션 컨텍스트가 완전히 초기화되고 모든 빈들이 로드된 후 실행됨
     * 즉, 애플리케이션이 시작되어 서비스 요청을 처리할 준비가 되면 실행됨
     *
     * @PostConstruct 와 다르게, Proxy 클래스의 생성도 마친 상태에서 실행되므로,
     * AOP가 적용된 클래스에 대해서도 작업이 가능하다.
     */
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAll() {
        User admin = User.builder()
                        .email(adminEmail)
                        .password(bCryptPasswordEncoder.encode(adminPwd))
                        .nickname(adminNickname)
                        .role("ROLE_ADMIN")
                        .point(0)
                        .earnedBetSucceedPoint(0)
                        .build();
        userRepository.save(admin);
        userPreferencesRepository.save(UserPreferences.of(admin));
        Map<String, Fighter> fighterNameMap = saveFighters();
        readFightEventInfoFromJsonFile(fighterNameMap);
        readSeedFAQJsonFile();
    }

    /**
     * {
     * "name": "Merab Dvalishvili",
     * "nickname": "The Machine",
     * "record": "20-4-0 (k NC)",
     * "height": "5' 6",
     * "weight": "135",
     * "birthday": "Jan 10, 1991",
     * "reach": "68",
     * "stance": "Orthodox"
     * },
     */
    private Map<String, Fighter> saveFighters() {
        List<Fighter> fightersToSave = new ArrayList<>();
        // 3-66
        JSONParser parser = new JSONParser();
        try {
            FileReader reader = new FileReader(ufcDataJsonPath);
            JSONObject jsonObj = (JSONObject) parser.parse(reader);
            JSONArray fighters = (JSONArray) jsonObj.get("fighters");
            int i = 0, j = 0;
            for (Object obj : fighters) {
                JSONObject fighterObj = (JSONObject) obj;
                String record = fighterObj.get("record").toString();
                String weight = fighterObj.get("weight").toString();
                String[] split_record = record.split("-");
                String nationalityStr = fighterObj.get("nationality") != null ? fighterObj.get("nationality").toString() : null;
                Country nationality = null;
                if (nationalityStr != null) {
                    try {
                        nationality = Country.valueOf(nationalityStr);
                    } catch (IllegalArgumentException e) {
                        log.error("e=",e);
                        throw new CustomException(ErrorCode.SERVER_ERROR_500);
                    }
                }
                Fighter fighter = Fighter.builder()
                        .name(fighterObj.get("name").toString())
                        .koreanName(fighterObj.get("korean_name") != null ? fighterObj.get("korean_name").toString() : null)
                        .nickname(fighterObj.get("nickname") != null ? fighterObj.get("nickname").toString() : null)
                        .nationality(nationality)
                        .height(!fighterObj.get("height").toString().contains("-") ?
                                toCentimeter(fighterObj.get("height").toString()) : 0)
                        .reach(!fighterObj.get("reach").toString().contains("-") ?
                                (int) (Integer.parseInt(fighterObj.get("reach").toString()) * 2.54 + 0.5) : 0)
                        .weight(!weight.contains("-") ? toKg(weight) : null)
//                        .division(!weight.contains("-") ? Fighter.get_division(weight) : null)
                        .birthday(!fighterObj.get("birthday").toString().contains("-") ? LocalDate.parse(fighterObj.get("birthday").toString(),
                                DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)) : null)
                        .fightRecord(FightRecord.builder()
                                .win(Integer.parseInt(split_record[0]))
                                .loss(Integer.parseInt(split_record[1]))
                                .draw(((int) split_record[2].charAt(0)) - 48)
                                .build())
                        .build();
                i++;
                fightersToSave.add(fighter);
//                if (i == 96)
//                    break;
            }
            fighterRepository.saveAll(fightersToSave);
        } catch (Exception e) {
            log.error("error=", e);
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
        return fightersToSave.stream()
                .collect(Collectors.toMap(Fighter::getName, f -> f));
    }

    private void readFightEventInfoFromJsonFile(Map<String, Fighter> fighterMap) {
        List<FightEvent> fightEventsToSave = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try {
            FileReader reader = new FileReader(ufcDataJsonPath);
            JSONObject jsonObj = (JSONObject) parser.parse(reader);
            JSONArray events = (JSONArray) jsonObj.get("events");
            List<JSONObject> eventList = new ArrayList<>();
            for (Object arr : events) {
                eventList.add((JSONObject) arr);
            }
            eventList.sort(Comparator.comparing(e ->
                    LocalDate.parse(
                            e.get("event_date").toString(),
                            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH)
                    )
            ));
            for (JSONObject eventObj : eventList) {
                LocalDate eventDate = LocalDate.parse(eventObj.get("event_date").toString(),
                        DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH));
                JSONArray cards = (JSONArray) eventObj.get("cards");
                FightEvent fightEvent = FightEvent.builder()
                        .name(eventObj.get("event_name").toString())
                        .eventDate(eventDate)
                        .displayDate(eventDate)
                        .location(eventObj.get("location").toString())
                        .completed(true)
                        .build();
                int cardOrder = 1;
                for (Object arr2 : cards) {
                    JSONObject cardObj = (JSONObject) arr2;
                    Fighter winner = fighterMap.get(cardObj.get("winner").toString());
                    Fighter loser = fighterMap.get(cardObj.get("loser").toString());
                    Object method = cardObj.get("method");
                    Object description = cardObj.get("description");
                    String[] timeParts = cardObj.get("fight_time").toString().split(":");
                    int round = Integer.parseInt(cardObj.get("round").toString());
                    FighterFightEvent fighterFightEvent = FighterFightEvent.builder()
                            .cardOrder(cardOrder++)
                            .winner(winner)
                            .loser(loser)
                            .title(cardObj.get("is_title") != null && Boolean.parseBoolean(cardObj.get("is_title").toString()))
                            .fotN(cardObj.get("is_fotn") != null && Boolean.parseBoolean(cardObj.get("is_fotn").toString()))
                            .potN(cardObj.get("is_potn") != null && Boolean.parseBoolean(cardObj.get("is_potn").toString()))
                            .fightWeight(FightWeight.fromDisplayName(cardObj.get("fight_weight").toString()))
                            .fightResult(FightResult.builder()
                                    .winMethod(method != null ?
                                            WinMethod.valueOf(method.toString()) : null)
                                    .winDescription(description != null ? description.toString() : null)
                                    .fightDuration(getFightDuration(round, timeParts))
                                    .round(round)
                                    .draw(Boolean.parseBoolean(cardObj.get("draw").toString()))
                                    .nc(Boolean.parseBoolean(cardObj.get("nc").toString()))
                                    .build())
                            .build();
                    fightEvent.addFighterFightEvent(fighterFightEvent);
                    fightEventsToSave.add(fightEvent);
                }
//                if (j == 20)
//                    break;
            }
        } catch (Exception e) {
            log.error("error=", e);
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
        fightEventRepository.saveAll(fightEventsToSave);
    }

    private void readSeedFAQJsonFile() {
        JSONParser parser = new JSONParser();
        try {
            FileReader reader = new FileReader(seedFaqJsonPath);
            JSONObject jsonObj = (JSONObject) parser.parse(reader);
            JSONArray faqs = (JSONArray) jsonObj.get("faqs");
            List<FAQ> faqList = new ArrayList<>();
            for (Object obj : faqs) {
                JSONObject faqJson = (JSONObject) obj;
                FAQ faq = FAQ.builder()
                        .question(faqJson.get("question").toString())
                        .answer(faqJson.get("answer").toString())
                        .faqCategory(FAQCategory.valueOf(faqJson.get("faqCategory").toString()))
                        .build();
                faqList.add(faq);
            }
            faqRepository.saveAll(faqList);
        } catch (IOException | ParseException e) {
            log.info("error=", e);
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
    }

}
