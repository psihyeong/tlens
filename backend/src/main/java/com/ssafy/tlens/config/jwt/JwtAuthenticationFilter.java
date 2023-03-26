package com.ssafy.tlens.config.jwt;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tlens.common.ResponseDto;
import com.ssafy.tlens.config.auth.PrincipalDetails;
import com.ssafy.tlens.dto.LoginRequestDto;
import com.ssafy.tlens.enums.LoginType;
import com.ssafy.tlens.enums.ResponseEnum;
import com.ssafy.tlens.handler.exception.CustomAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 로그인 인증과정
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter{

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    // Authentication 객체 만들어서 리턴 => 의존 : AuthenticationManager
    // 인증 요청시에 실행되는 함수 => /login
    // refresh, login
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        System.out.println("JwtAuthenticationFilter : 진입");

        // request에 있는 username과 password를 파싱해서 자바 Object로 받기
        ObjectMapper om = new ObjectMapper();
        LoginRequestDto loginRequestDto = null;
        try {
            loginRequestDto = om.readValue(request.getInputStream(), LoginRequestDto.class);
        } catch (Exception e) {
            throw new CustomAuthenticationException(ResponseEnum.AUTH_BAD_REQUEST);
        }

        System.out.println("JwtAuthenticationFilter : "+loginRequestDto);

        String userEmail = null;

        if(loginRequestDto.getLoginType().equals(LoginType.LOGIN)){
            // 로그인
//            KakaoUserInfoDto kakaoUserInfoDto = kakaoProvider.login(loginRequestDto.getToken());
//            if(kakaoUserInfoDto == null){
//                throw new CustomAuthenticationException(ResponseEnum.AUTH_INVALID_TOKEN);
//            }
            userEmail = loginRequestDto.getEmail();
        }else{
            // refresh
            String refreshToken = redisTemplate.opsForValue().getAndDelete(loginRequestDto.getToken());
            if(refreshToken == null){
                throw new CustomAuthenticationException(ResponseEnum.AUTH_REFRESH_DOES_NOT_EXIST);
            }
            userEmail = jwtProvider.getUserEmail(refreshToken);
        }

        // 유저네임패스워드 토큰 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        userEmail,
                        loginRequestDto.getPassword());

        System.out.println("JwtAuthenticationFilter : 토큰생성완료");

        // authenticate() 함수가 호출 되면 인증 프로바이더가 유저 디테일 서비스의
        // loadUserByUsername(토큰의 첫번째 파라메터) 를 호출하고
        // UserDetails를 리턴받아서 토큰의 두번째 파라메터(credential)과
        // UserDetails(DB값)의 getPassword()함수로 비교해서 동일하면
        // Authentication 객체를 만들어서 필터체인으로 리턴해준다.

        // Tip: 인증 프로바이더의 디폴트 서비스는 UserDetailsService 타입
        // Tip: 인증 프로바이더의 디폴트 암호화 방식은 BCryptPasswordEncoder
        // 결론은 인증 프로바이더에게 알려줄 필요가 없음.
        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        }catch (Exception e){
            e.printStackTrace();
            throw new CustomAuthenticationException(ResponseEnum.AUTH_NOT_JOINED);
        }

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        System.out.println("Authentication : "+principalDetails.getUser().getEmail());
        return authentication;
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        CustomAuthenticationException exception = ((CustomAuthenticationException) failed);
        ObjectMapper om = new ObjectMapper();
        String responseBody = om.writer().writeValueAsString(new ResponseDto<>(exception.getResponseEnum()));
        response.getWriter().println(responseBody);
        response.setStatus(exception.getResponseEnum().getCode());
        response.setContentType("application/json");
    }

    // JWT Token 생성해서 response에 담아주기
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        System.out.println("successfulAuthentication");

        PrincipalDetails principalDetails = (PrincipalDetails) authResult.getPrincipal();
        String accessToken = jwtProvider.createAccessToken(principalDetails.getUser().getUserId(),principalDetails.getUser().getEmail());
        String refreshToken = jwtProvider.createRefreshToken(principalDetails.getUser().getUserId(),principalDetails.getUser().getEmail());
        redisTemplate.opsForValue().set(accessToken, refreshToken);

        response.addHeader(JwtProperties.HEADER_STRING, JwtProperties.TOKEN_PREFIX+accessToken);
    }

}