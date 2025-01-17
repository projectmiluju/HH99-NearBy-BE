package com.hh99.nearby.login.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hh99.nearby.entity.Member;
import com.hh99.nearby.login.dto.request.KakaoRequestDto;
import com.hh99.nearby.login.dto.request.KakaocodeDto;
import com.hh99.nearby.login.dto.response.KakaoResponseDto;
import com.hh99.nearby.login.dto.response.LoginResponseDto;
import com.hh99.nearby.repository.MemberRepository;
import com.hh99.nearby.security.UserDetailsImpl;
import com.hh99.nearby.security.dto.TokenDto;
import com.hh99.nearby.security.jwt.TokenProvider;
import com.hh99.nearby.util.LevelCheck;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final TokenProvider tokenProvider;

    private final MemberRepository memberRepository;

    private final LevelCheck levelCheck;

    @Value("${kakao.client.id}")
    String restapikey;

    @Value("${kakao.redirect.url}")
    String url;

    @Value("${kakao.url}")
    String url2;


    public ResponseEntity<?> kakaoLogin(KakaocodeDto kakaocode, HttpServletResponse response) throws JsonProcessingException {

        //"인가 코드"로 "액세스 토큰" 요청
        String accessToken = getAccessToken(kakaocode.getCode());

        //토큰으로 카카오 API 호출
        KakaoRequestDto kakaoUserInfo = getKakaoUserInfo(accessToken); //엑세스 토큰값으로 유저 정보 받아오기!

        Optional<Member> member = memberRepository.findByKakaoId(kakaoUserInfo.getKakaoid()); //가입이 되어있는지 확인
        if (member.isPresent()) {
            Member kakaoUser = Member.builder()
                    .kakaoId(kakaoUserInfo.getKakaoid())
                    .nickname(kakaoUserInfo.getNickname())
                    .profileImg(kakaoUserInfo.getProfileImg())
                    .build();
            Authentication authentication = forceLogin(member.get());

            //response Header에 JWT 토큰 추가
            kakaoUsersAuthorizationInput(member.get(), authentication, response);

            List<Long> levelAndPoint = levelCheck.levelAndPoint(kakaoUser.getNickname()); // 레벨 계산
            LoginResponseDto loginResponseDto = LoginResponseDto.builder()
                    .profileImg(member.get().getProfileImg())
                    .nickname(member.get().getNickname())
                    .level("LV."+levelAndPoint.get(1))
                    .remainingTime((levelAndPoint.get(0)/10 % 70) )
                    .totalTime(levelAndPoint.get(0)/10+"분")
                    .build();

            return ResponseEntity.ok().body(Map.of("logincheck",true,"msg","로그인성공","data",loginResponseDto)); //로그인처리
        }
            KakaoResponseDto responseDto = KakaoResponseDto.builder() //카카오톡id 파싱
                    .kakaoId(kakaoUserInfo.getKakaoid())
                    .profileImg(kakaoUserInfo.getProfileImg())
                    .build();

            return ResponseEntity.ok().body(Map.of("logincheck",false,"msg","닉네임 설정이 필요합니다.","data",responseDto ));
    }

    //"인가 코드"로 "액세스 토큰" 요청
    private String getAccessToken(String code) throws JsonProcessingException { //인가코드 파라미터
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders(); // 헤더를 생성
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");//헤더에 값을 넣어줌

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>(); //바디는 키:벨류로 데이터를 보내서 MultivalueMap 사용
        body.add("grant_type", "authorization_code"); //grant_type
        body.add("client_id", restapikey); //내 restapi 키
        body.add("redirect_uri", url); //리다이렉트 Url
        body.add("code", code); //카카오로 받는 인가코드

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();//서버에게 요청을 보냄
        ResponseEntity<String> response = rt.exchange(
                url2,
                HttpMethod.POST, //매서드는 포스트타입
                kakaoTokenRequest, // 카카오서버로 보낼 httpEntity
                String.class //스트링 클래스로
        ); //카카오 데이터 받음

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody(); //바디 부분
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody); //Json형태를 객체로 바꾸기
        return jsonNode.get("access_token").asText(); //엑세스토큰 받은거 넘기기
    }

    // 2. 토큰으로 카카오 API 호출
    private KakaoRequestDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders(); // 헤더넣는 객체
        headers.add("Authorization", "Bearer " + accessToken); //엑세스 토큰 값
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8"); //타입

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();//서버에게 요청을 보냄
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,  //매서드는 포스트타입
                kakaoUserInfoRequest, // 카카오서버로 보낼 httpEntity
                String.class //스트링 클래스로
        );

        // responseBody에 있는 정보를 꺼냄
        String responseBody = response.getBody(); //바디에 정보를 꺼냄
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody); //Json형태를 객체로 바꾸기

        Long id = jsonNode.get("id").asLong(); // 아이디
        String nickname = jsonNode.get("properties").get("nickname").asText();// 닉네임
        String profileImg = jsonNode.get("properties").get("profile_image").asText(); //프로필url

        return new KakaoRequestDto(id, nickname, profileImg); //Dto 에 담아서 리턴
    }
    //강제 로그인 처리
    private Authentication forceLogin(Member kakaoUser) {
        UserDetails userDetails = new UserDetailsImpl(kakaoUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    //response Header에 JWT 토큰 추가
    private void kakaoUsersAuthorizationInput(Member kakaoUser, Authentication authentication, HttpServletResponse response) {
        // response header에 token 추가
        TokenDto token = tokenProvider.generateTokenDto(kakaoUser);//.generateJwtToken(userDetailsImpl);
        response.addHeader("Authorization", "Bearer" + " " + token.getAccessToken());
    }
}