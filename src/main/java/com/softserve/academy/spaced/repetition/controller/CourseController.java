package com.softserve.academy.spaced.repetition.controller;


import com.softserve.academy.spaced.repetition.controller.dto.annotations.Request;
import com.softserve.academy.spaced.repetition.controller.dto.builder.DTOBuilder;
import com.softserve.academy.spaced.repetition.controller.dto.impl.CourseLinkDTO;
import com.softserve.academy.spaced.repetition.controller.dto.impl.CoursePublicDTO;
import com.softserve.academy.spaced.repetition.domain.Course;
import com.softserve.academy.spaced.repetition.service.CourseService;
import com.softserve.academy.spaced.repetition.service.UserService;
import com.softserve.academy.spaced.repetition.utils.audit.Auditable;
import com.softserve.academy.spaced.repetition.utils.audit.AuditingAction;
import com.softserve.academy.spaced.repetition.utils.exceptions.NotAuthorisedUserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Auditable(action = AuditingAction.VIEW_COURSES_VIA_CATEGORY)
    @GetMapping(value = "/api/categories/{category_id}/courses")
    public ResponseEntity<Page<CourseLinkDTO>> getAllCoursesByCategoryId(@PathVariable Long category_id,
                                                                         @RequestParam(name = "p", defaultValue = "1")
                                                                                 int pageNumber,
                                                                         @RequestParam(name = "sortBy") String sortBy,
                                                                         @RequestParam(name = "asc") boolean ascending) {
        Page<CourseLinkDTO> courseLinkDTOS = courseService
                .getPageWithCoursesByCategory(category_id, pageNumber, sortBy, ascending).map((course) -> {
                    Link selfLink = linkTo(methodOn(CourseController.class)
                            .getAllCoursesByCategoryId(category_id, pageNumber, sortBy, ascending)).withSelfRel();
                    return DTOBuilder.buildDtoForEntity(course, CourseLinkDTO.class, selfLink);
                });
        return new ResponseEntity<>(courseLinkDTOS, HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.VIEW_COURSES)
    @GetMapping(value = "/api/courses")
    public ResponseEntity<Page<CourseLinkDTO>> getAllCourses(@RequestParam(name = "p", defaultValue = "1") int pageNumber,
                                                             @RequestParam(name = "sortBy") String sortBy,
                                                             @RequestParam(name = "asc") boolean ascending) {
        Page<CourseLinkDTO> courseLinkDTOS = courseService
                .getPageWithCourses(pageNumber, sortBy, ascending).map((course) -> {
                    Link selfLink = linkTo(methodOn(CourseController.class)
                            .getCourseById(course.getId())).withSelfRel();
                    return DTOBuilder.buildDtoForEntity(course, CourseLinkDTO.class, selfLink);
                });
        return new ResponseEntity<>(courseLinkDTOS, HttpStatus.OK);
    }

    @GetMapping(value = "/api/courses/orders")
    public ResponseEntity<List<CourseLinkDTO>> getAllCoursesOrderByRating() {
        List<Course> courseList = courseService.getAllOrderedCourses();
        Link collectionLink = linkTo(methodOn(CourseController.class).getAllCoursesOrderByRating()).withRel("courses");
        List<CourseLinkDTO> decks = DTOBuilder
                .buildDtoListForCollection(courseList, CourseLinkDTO.class, collectionLink);
        return new ResponseEntity<>(decks, HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.VIEW_TOP_COURSES)
    @GetMapping("/api/courses/top")
    public ResponseEntity<List<CourseLinkDTO>> getTopCourse() {
        List<Course> courseList = courseService.getTopCourse();
        List<CourseLinkDTO> courses = new ArrayList<>();
        for (Course course : courseList) {
            Link selfLink = linkTo(methodOn(CourseController.class)
                    .getCourseById(course.getId())).withSelfRel();
            courses.add(DTOBuilder.buildDtoForEntity(course, CourseLinkDTO.class, selfLink));
        }
        return new ResponseEntity<>(courses, HttpStatus.OK);
    }

    @GetMapping(value = "/api/courses/{course_id}")
    public ResponseEntity<CourseLinkDTO> getCourseById(@PathVariable Long course_id) {
        Course course = courseService.getCourseById(course_id);
        Link selfLink = linkTo(methodOn(CourseController.class).getCourseById(course_id)).withSelfRel();
        CourseLinkDTO linkDTO = DTOBuilder.buildDtoForEntity(course, CourseLinkDTO.class, selfLink);
        return new ResponseEntity<>(linkDTO, HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.CREATE_COURSE)
    @PostMapping(value = "/api/courses")
    public ResponseEntity<CoursePublicDTO> addCourse(@RequestBody Course course, @PathVariable Long category_id) {
        courseService.addCourse(course, category_id);
        Link selfLink = linkTo(methodOn(CourseController.class).getCourseById(course.getId())).withSelfRel();
        CoursePublicDTO coursePublicDTO = DTOBuilder.buildDtoForEntity(course, CoursePublicDTO.class, selfLink);
        return new ResponseEntity<>(coursePublicDTO, HttpStatus.CREATED);
    }

    @Auditable(action = AuditingAction.CREATE_COURSE)
    @PutMapping(value = "/api/cabinet/courses/{course_id}")
    public ResponseEntity updateCourse(@PathVariable Long course_id, @Validated(Request.class) @RequestBody Course course) {
        courseService.updateCourse(course_id, course);
        return new ResponseEntity(HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.DELETE_COURSE)
    @DeleteMapping(value = "/api/cabinet/global/courses/{course_id}")
    public ResponseEntity deleteGlobalCourse(@PathVariable Long course_id) throws NotAuthorisedUserException {
        courseService.deleteGlobalCourse(course_id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @DeleteMapping(value = "/api/cabinet/local/courses/{course_id}")
    public ResponseEntity deleteLocalCourse(@PathVariable Long course_id) throws NotAuthorisedUserException {
        courseService.deleteLocalCourse(course_id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.ADD_COURSE)
    @PutMapping("/api/cabinet/courses/{course_id}")
    public ResponseEntity addCourse(@PathVariable Long course_id) throws NotAuthorisedUserException {
        courseService.updateListOfCoursesOfTheAuthorizedUser(course_id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.ADD_COURSE)
    @PutMapping("/api/cabinet/{course_id}/update/access")
    public ResponseEntity updateCourseAccess(@PathVariable Long course_id,
                                             @Validated(Request.class) @RequestBody Course course) {
        courseService.updateCourseAccess(course_id, course);
        return new ResponseEntity(HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.ADD_COURSE)
    @PutMapping("/api/categories/courses/{courseId}/decks/{deckId}")
    public ResponseEntity addDeckToCourse(@Validated(Request.class) @PathVariable Long courseId,
                                          @PathVariable Long deckId) {
        courseService.addDeckToCourse(courseId, deckId);
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping("/api/private/cabinet/courses")
    public ResponseEntity<List<Long>> getIdAllCoursesOfTheCurrentUser() throws NotAuthorisedUserException {
        List<Long> id = courseService.getAllCoursesIdOfTheCurrentUser();
        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    @Auditable(action = AuditingAction.CREATE_PRIVATE_COURSE)
    @PostMapping("/api/categories/{category_id}/courses")
    public ResponseEntity<Course> createPrivateCourse(@Validated(Request.class) @RequestBody Course privateCourse,
                                                      @PathVariable Long category_id) throws NotAuthorisedUserException {
        courseService.createPrivateCourse(privateCourse, category_id);
        return new ResponseEntity<>(privateCourse, HttpStatus.OK);
    }
}
