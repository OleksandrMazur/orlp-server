package com.softserve.academy.spaced.repetition.dto.impl;

import com.softserve.academy.spaced.repetition.dto.DTO;
import com.softserve.academy.spaced.repetition.domain.Comment;
import com.softserve.academy.spaced.repetition.domain.CourseComment;
import org.springframework.hateoas.Link;

public class ReplyToCommentDTO {

    private String commentText;
    private Long parentCommentId;

    public String getCommentText() {
        return commentText;
    }

    public Long getParentCommentId() { return parentCommentId; }
}
