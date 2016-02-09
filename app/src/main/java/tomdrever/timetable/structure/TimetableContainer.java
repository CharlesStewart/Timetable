package tomdrever.timetable.structure;

import java.util.Date;

import tomdrever.timetable.structure.Timetable;

public class TimetableContainer {
    public TimetableContainer(String name, String description, Date dateCreated, Timetable timetable) {
        this.name = name;
        this.description = description;
        this.dateCreated = dateCreated;
        this.timetable = timetable;
    }

    public String name;
    public String description;
    public Date dateCreated;
    public Timetable timetable;
}