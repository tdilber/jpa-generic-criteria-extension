package com.beyt.jdq.presetation;

import com.beyt.jdq.BaseTestInstance;
import com.beyt.jdq.TestApplication;
import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.CriteriaList;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import com.beyt.jdq.testenv.entity.school.Course;
import com.beyt.jdq.testenv.repository.CourseRepository;
import com.beyt.jdq.testenv.repository.DepartmentRepository;
import com.beyt.jdq.util.PresentationUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(classes = TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S2_Multi_Value_Operators extends BaseTestInstance {
    private @Autowired CourseRepository courseRepository;
    private @Autowired DepartmentRepository departmentRepository;


    @Test
    public void equal() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.name, CriteriaOperator.EQUAL, "Calculus I", "Calculus II"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList); //2,3
        assertEquals(List.of(course2, course3), courseList);
    }

    @Test
    public void equalInteger() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.maxStudentCount, CriteriaOperator.EQUAL, 40, 50));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//1,6
        assertEquals(List.of(course1, course6), courseList);
    }

    @Test
    public void notEqual() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.name, CriteriaOperator.NOT_EQUAL, "Calculus I", "Calculus II"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//1,4,5,6,7,8,9,10
        assertEquals(Arrays.asList(course1, course4, course5, course6, course7, course8, course9, course10), courseList);
    }

    @Test
    public void notEqualDate() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.startDate, CriteriaOperator.NOT_EQUAL, "2013-06-18", "2015-06-18", "2016-06-18"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);
    }

    @Test
    public void in() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.name, CriteriaOperator.IN, "Calculus I", "Calculus II"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//2,3
        assertEquals(List.of(course2, course3), courseList);
    }

    @Test
    public void notIn() {
        var criteriaList = CriteriaList.of(Criteria.of(Course.Fields.name, CriteriaOperator.NOT_IN, "Calculus I", "Calculus II"));
        PresentationUtil.prettyPrint(criteriaList);
        List<Course> courseList = courseRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(courseList);//1,4,5,6,7,8,9,10
        assertEquals(Arrays.asList(course1, course4, course5, course6, course7, course8, course9, course10), courseList);
    }

}
