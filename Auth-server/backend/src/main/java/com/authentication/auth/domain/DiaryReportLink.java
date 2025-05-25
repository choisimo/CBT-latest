package com.authentication.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Diary_Report_Link")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryReportLink {

    @EmbeddedId
    private DiaryReportLinkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("diaryId") // This maps diaryId from DiaryReportLinkId to the Diary entity
    @JoinColumn(name = "diary_id")
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reportId") // This maps reportId from DiaryReportLinkId to the Report entity
    @JoinColumn(name = "report_id")
    private Report report;

}
