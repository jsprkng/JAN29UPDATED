package com.mycompany.donezodraft.InternalFrames;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;

public class Workflow extends JInternalFrame {

    private static final int ROWS = 24 * 4; // 24 hours * 4 intervals per hour to support 15mins interval
    private static final int COLUMNS = 8;
    private static final int CELL_WIDTH = 120;
    private static final int CELL_HEIGHT = 60 / 4; // 60 minutes / 4 intervals
    private static final int PANEL_WIDTH = COLUMNS * CELL_WIDTH;
    private static final int PANEL_HEIGHT = ROWS * CELL_HEIGHT;
    private static final ArrayList<Schedule> scheduleList = new ArrayList<>();
    private static final String SCHEDFILE_PATH = "Scheddatabase.txt";
    private static final String TASKFILE_PATH = "database.txt";
    private final ArrayList<Task> taskList = new ArrayList<>();
    private final boolean[][] taskConflict = new boolean[ROWS][COLUMNS - 1];

    private JPanel container;

    public Workflow() {
        super("Weekly Schedule", false, false, false, false);
        setSize(979, 693);
        setLayout(new BorderLayout());
        setBorder(null);
        setBackground(new Color(240, 237, 237));

        BasicInternalFrameUI internalFrameUI = (BasicInternalFrameUI) this.getUI();
        internalFrameUI.setNorthPane(null);

        JPanel northPanel = createNorthPanel();
        add(northPanel, BorderLayout.NORTH);

        JPanel timeLabelsPanel = createTimeLabelsPanel();

        container = new JPanel(null);
        container.setBackground(new Color(240, 240, 240));
        container.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        JScrollPane timeScrollPane = createScrollPane(timeLabelsPanel, false);
        JScrollPane scheduleScrollPane = createScrollPane(container, true);
        linkScrollBars(timeScrollPane, scheduleScrollPane);

        add(timeScrollPane, BorderLayout.WEST);
        add(scheduleScrollPane, BorderLayout.CENTER);

        
        scheduleList.clear();
        scheduleList.addAll(ScheduleFileHandler.funcReadFile(SCHEDFILE_PATH));
        taskList.clear();
        taskList.addAll(FileH.funcReadFile(TASKFILE_PATH));
        loadSched();

        setVisible(true);
    }

    private JPanel createNorthPanel() {
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(245, 245, 245));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(245, 245, 245));

        JLabel titleLabel = new JLabel("Workflow - " + getCurrentWeekRange(), SwingConstants.LEFT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(33, 37, 41));
        titleLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        RoundedPanel headerPanel = new RoundedPanel();
        headerPanel.setLayout(new GridLayout(1, COLUMNS));
        headerPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 30));
        headerPanel.setBackground(Color.WHITE);

        String[] days = {"", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < days.length; i++) {
            JLabel label = new JLabel(days[i], SwingConstants.CENTER);
            label.setOpaque(true);

            if (i == 0) {
                label.setBackground(new Color(240, 237, 237)); 
            } else {
                label.setBackground(Color.WHITE);
            }

            label.setBorder(null);
            label.setFont(new Font("SansSerif", Font.BOLD, 14));
            headerPanel.add(label);
        }

        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(headerPanel, BorderLayout.SOUTH);
        return northPanel;
    }

    private JPanel createTimeLabelsPanel() {
        RoundedPanel timeLabelsPanel = new RoundedPanel();
        timeLabelsPanel.setLayout(new GridLayout(ROWS / 4, 1));
        timeLabelsPanel.setPreferredSize(new Dimension(120, PANEL_HEIGHT));
        timeLabelsPanel.setBackground(Color.WHITE);

        for (int i = 0; i < ROWS / 4; i++) {
            String timeLabel;

            if (i == 0) {
                timeLabel = "12 MN";
            } else if (i < 12) {
                timeLabel = i + " AM";
            } else if (i == 12) {
                timeLabel = "12 NN";
            } else {
                timeLabel = (i - 12) + " PM";
            }

            JLabel label = new JLabel(timeLabel, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(Color.WHITE);
            label.setBorder(null);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            timeLabelsPanel.add(label);
        }

        return timeLabelsPanel;
    }

    private JScrollPane createScrollPane(JPanel panel, boolean verticalScroll) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(verticalScroll ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        return scrollPane;
    }

    private void linkScrollBars(JScrollPane timeScrollPane, JScrollPane scheduleScrollPane) {
        scheduleScrollPane.getVerticalScrollBar().addAdjustmentListener(e ->
                timeScrollPane.getVerticalScrollBar().setValue(e.getValue())
        );
    }

    private String getCurrentWeekRange() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d");
        return monday.format(formatter) + " - " + sunday.format(formatter);
    }

    private void loadSched() {
        // Load schedules
        for (Schedule schedule : scheduleList) {
            String reason = schedule.getReason();
            int column = schedule.getIntDay() - 1; // Adjust for 0-based indexing
    
            // Validate column
            if (column < 0 || column > 7) {
                continue; // Invalid day sched
            }
    
            int startTime = schedule.getStartTime();
            int endTime = schedule.getEndTime();
    
            // Handle schedules that span midnight
            if (startTime > endTime) {
                // Split into two parts: startTime to 2359 and 0000 to endTime
                int startRow1 = (startTime / 100) * 4 + ((startTime % 100) / 15);
                int endRow1 = 95; // 23:59 is the last row (23 * 4 + 3 = 95)
    
                int startRow2 = 0; // 00:00 is the first row
                int endRow2 = (endTime / 100) * 4 + ((endTime % 100) / 15);
    
                // Add the first part (11 PM to 11:59 PM)
                if (!hasConflict(column, startRow1, endRow1)) {
                    addPanel(column, startRow1, endRow1, reason, 4);
                    markTimeSlots(column, startRow1, endRow1, true);
                }
    
                // Add the second part (12 AM to 12 AM)
                if (!hasConflict(column, startRow2, endRow2)) {
                    addPanel(column, startRow2, endRow2, reason, 4);
                    markTimeSlots(column, startRow2, endRow2, true);
                }
            } 
            else {
                // Normal schedule (does not span midnight)
                int startRow = (startTime / 100) * 4 + ((startTime % 100) / 15);
                int endRow = (endTime / 100) * 4 + ((endTime % 100) / 15);
    
                // Validate startRow and endRow
                if (startRow < 0 || startRow >= ROWS || endRow < 0 || endRow >= ROWS || startRow > endRow) {
                    continue; // Invalid time
                }
    
                if (column == 7) {
                    // Add to all columns (maybe for all days?)
                    for (int i = 0; i < 7; i++) {
                        if (!hasConflict(i, startRow, endRow)) { // Will not print conflicting scheds
                            addPanel(i, startRow, endRow, reason, 4);
                            markTimeSlots(i, startRow, endRow, true);
                        }
                    }
                } else {
                    // For frequency that is not everyday
                    if (!hasConflict(column, startRow, endRow)) { // Will not print conflicting scheds
                        addPanel(column, startRow, endRow, reason, 4);
                        markTimeSlots(column, startRow, endRow, true);
                    }
                }
            }
        }
    
        TaskSorter taskSorter = new TaskSorter(); 
        taskSorter.sortTasks(taskList); //Sorting the tasks using algo
        LocalDate currentDate = LocalDate.now();
        // Load tasks
        for (Task task : taskList) {
            if(task.getProgress().equals("Completed")){ //Completed tasks are ignored
                continue;
            }
            int taskColor = 0;
            LocalDate dueDate = task.getDueDate();
            long daysLeft = ChronoUnit.DAYS.between(currentDate, dueDate);
            if(task.getDifficulty().equals("Easy")){// For do immediately, start early, schedule later
                if(daysLeft <= 1)
                    taskColor = 1; //Red for do immediately
                else if(daysLeft <= 6 && daysLeft >= 2)
                    taskColor = 2; //Yellow for start early
                else
                    taskColor = 3; //Green for sched later
            }
            else if(task.getDifficulty().equals("Medium")){
                if(daysLeft <= 4)
                    taskColor = 1;  
                else if(daysLeft <= 5 && daysLeft >= 14)
                    taskColor = 2;
                else
                    taskColor = 3;
            }
            else if(task.getDifficulty().equals("Hard")){
                if(daysLeft <= 7)
                    taskColor = 1;
                else
                    taskColor = 2;
            }
            String reason = task.getName();
            
            int timeAllotted = task.getTimeAllotted(); 
            int intervals = (int) Math.ceil(timeAllotted / 15.0); // Convert minutes to 15-minute intervals (rounded up)
            boolean taskAdded = false;
    
            for (int column = 0; column < 7; column++) {
                int intervalsChecker = 0; //To fit the task time in the sched
                int startRow = 0;
    
                for (int row = 0; row < ROWS; row++) {
                    if (!taskConflict[row][column]) {
                        if (intervalsChecker == 0) {
                            startRow = row; // Mark the start of a potential time slot
                        }
                        intervalsChecker++;
    
                        if (intervalsChecker == intervals) {
                            // Validate startRow and endRow
                            int endRow = startRow + intervals - 1;
                            if (endRow >= ROWS) {
                                break; // Skip if the task exceeds the day's time slots
                            }
    
                            // Found enough consecutive time slots
                            addPanel(column, startRow, endRow, reason, taskColor);
                            markTimeSlots(column, startRow, endRow, true);
                            taskAdded = true;
                            break;
                        }
                    } else {
                        intervalsChecker = 0; // Reset if a conflict is found
                    }
                }
    
                if (taskAdded) {
                    break; // Move to the next task
                }
            }
        }
    }
    private boolean hasConflict(int column, int startRow, int endRow) {
        for (int row = startRow; row <= endRow; row++) {
            if (taskConflict[row][column]) {
                return true; // Conflict found
            }
        }
        return false; // No conflict
    }
    private void markTimeSlots(int column, int startRow, int endRow, boolean occupied) {
        for (int row = startRow; row <= endRow; row++) {
            taskConflict[row][column] = occupied;
        }
    }

    private void addPanel(int column, int startRow, int endRow, String reason, int color) {
        int x = (column * CELL_WIDTH); // Offset by column
        int y = startRow * CELL_HEIGHT;
        int height = (endRow - startRow + 1) * CELL_HEIGHT;
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout());
        switch (color) {
            case 1:
                panel.setBackground(new Color(255, 218, 218)); //For Do Immediately
                break;
            case 2:
                panel.setBackground(new Color(220, 218, 255)); //For Start Early
                break;
            case 3:
                panel.setBackground(new Color(218, 255, 218)); //For Schedule Later
                break;
            case 4:      
                panel.setBackground(new Color(255, 253, 218)); //For Customized Scheds
                break;
            default:
                break;
        }
        
        panel.setBounds(x, y, CELL_WIDTH, height); 
    
        JLabel label = new JLabel("<html>" + reason + "</html>", SwingConstants.CENTER);
            try {
                InputStream outfitFontStream = getClass().getResourceAsStream("/fontStyles/Outfit-ExtraBold.ttf");
                Font outfitFont = Font.createFont(Font.TRUETYPE_FONT, outfitFontStream).deriveFont(Font.BOLD, 12f);
                label.setFont(outfitFont);
            } catch (Exception e) {
                e.printStackTrace();
            }
        panel.add(label);
    
        container.add(panel);
        container.repaint();
    }
}
