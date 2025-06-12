package com.authentication.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryReportLinkId implements Serializable {

    @Column(name = "diary_id")
    private Long diaryId;

    @Column(name = "report_id")
    private Long reportId;
}
