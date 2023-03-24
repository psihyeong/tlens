package com.ssafy.tlens.api.service;

import com.ssafy.tlens.api.request.ScrapRequestDTO;
import com.ssafy.tlens.common.exception.handler.DuplicateResourceException;
import com.ssafy.tlens.common.exception.handler.NotFoundException;
import com.ssafy.tlens.entity.rdbms.*;
import com.ssafy.tlens.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScrapServiceImpl implements ScrapService {

    private final UserRepository userRepository;
    private final NewsRepository newsrepository;
    private final ScrapRepository scrapRepository;
    @Override
    @Transactional
    public void insert(ScrapRequestDTO scrapRequestDTO) {
        User user = userRepository.findById(scrapRequestDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("Could not found user id : " + scrapRequestDTO.getUserId()));

        News news = newsrepository.findById(scrapRequestDTO.getNewsId())
                .orElseThrow(() -> new NotFoundException("Could not found news id : " + scrapRequestDTO.getNewsId()));

        // 이미 스크랩되어있으면 에러 반환
        if (scrapRepository.findByUserAndNews(user, news).isPresent()){
            // TODO 409에러로 변경
            throw new DuplicateResourceException("already exist data by user id :" + user.getUserId() + " ,"
                    + "news id : " + news.getNewsId());
        }

        Scrap scrap = Scrap.builder()
                .user(user)
                .news(news)
                .build();

        scrapRepository.save(scrap);
    };

    @Override
    @Transactional
    public void delete(ScrapRequestDTO scrapRequestDTO) {
        User user = userRepository.findById(scrapRequestDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("Could not found user id : " + scrapRequestDTO.getUserId()));

        News news = newsrepository.findById(scrapRequestDTO.getNewsId())
                .orElseThrow(() -> new NotFoundException("Could not found news id : " + scrapRequestDTO.getNewsId()));

        Scrap scrap = scrapRepository.findByUserAndNews(user, news)
                .orElseThrow(() -> new NotFoundException("Could not found scrap id"));

        scrapRepository.delete(scrap);
    };
}
