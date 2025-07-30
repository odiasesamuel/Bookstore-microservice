package com.prunny.reviewservice.service.mapper;

import com.prunny.reviewservice.domain.Review;
import com.prunny.reviewservice.service.dto.ReviewDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Review} and its DTO {@link ReviewDTO}.
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper extends EntityMapper<ReviewDTO, Review> {}
