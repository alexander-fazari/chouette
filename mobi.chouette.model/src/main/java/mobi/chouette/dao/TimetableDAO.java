package mobi.chouette.dao;

import java.util.Collection;

import mobi.chouette.model.Timetable;
import mobi.chouette.model.statistics.LineAndTimetable;

public interface TimetableDAO extends GenericDAO<Timetable> {

	Collection<LineAndTimetable> getAllTimetableForAllLines();
}
