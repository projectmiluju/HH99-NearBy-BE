package com.hh99.nearby.detail.dto;

import com.hh99.nearby.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DetailResponseDto {
    private String title;
    private String challengeImg;
    private LocalDate startDay;
    private LocalTime startTime;
    private Long targetTime;
    private LocalDateTime endTime;
    private Long limitPeople;
    private String content;
    private String notice;
    private String writer;

}