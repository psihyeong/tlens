package com.ssafy.tlens.api.service;


import com.ssafy.tlens.api.request.TrendRequestDTO;
import com.ssafy.tlens.api.response.MainPressDTO;
import com.ssafy.tlens.api.response.ReporterInfoDTO;
import com.ssafy.tlens.common.exception.handler.NotFoundException;
import com.ssafy.tlens.entity.rdbms.Press;
import com.ssafy.tlens.entity.rdbms.Reporter;
import com.ssafy.tlens.entity.rdbms.ReporterTrend;
import com.ssafy.tlens.repository.PressRepository;
import com.ssafy.tlens.repository.ReporterRepository;
import com.ssafy.tlens.repository.ReporterTrendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporterServiceImpl implements ReporterService {

    private ReporterTrendRepository reporterTrendRepository;
    private ReporterRepository reporterRepository;
    private PressRepository pressRepository;

    public void insertToReporter(TrendRequestDTO request) {

        Reporter reporter = reporterRepository.findById(request.getTargetId())
                .orElseThrow(() -> new NotFoundException("Could not found reporter id : " + request.getTargetId()));

        ReporterTrend reporterTrend = ReporterTrend.builder()
                .keyword(request.getKeyword())
                .count(request.getCount())
                .date(request.getDate())
                .reporter(reporter)
                .build();

        reporterTrendRepository.save(reporterTrend);
    }

    public void updateToReporter(TrendRequestDTO request) {

        ReporterTrend trend = reporterTrendRepository.findById(request.getTargetId())
                .orElseThrow(() -> new NotFoundException("Could not found trend id : " + request.getTargetId()));

        trend.update(request.getKeyword(),request.getCount(),request.getDate());
    }

    public void deleteToReporter(Long id) {

        ReporterTrend trend = reporterTrendRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Could not found trend id : " + id));

        reporterTrendRepository.delete(trend);
    }

    public List<ReporterInfoDTO> getReportersByPress(Long pressId) {

        Press press = pressRepository.findById(pressId)
                .orElseThrow(() -> new NotFoundException("Could not found press id : " + pressId));

        List<Reporter> reporters = press.getReporters();

        List<ReporterInfoDTO> reporterInfoList = reporters.stream()
                .map(reporter -> new ReporterInfoDTO(reporter))
                .collect(Collectors.toList());

        return reporterInfoList;
    }
}