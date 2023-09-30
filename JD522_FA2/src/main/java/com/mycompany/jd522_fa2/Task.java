/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.jd522_fa2;
import java.io.Serializable;

/**
 *
 * @author Xandr
 */
public class Task<T> implements Serializable{
    private int taskId;
    private T taskName;
    private T description;
    private T completionStatus;
    private T category;

    public Task(int taskId, T taskName, T description, T completionStatus, T category) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.completionStatus = completionStatus;
        this.category = category;
    }

    // Getters and setters for task properties
    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public T getTaskName() {
        return taskName;
    }

    public void setTaskName(T taskName) {
        this.taskName = taskName;
    }

    public T getDescription() {
        return description;
    }

    public void setDescription(T description) {
        this.description = description;
    }

    public T getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(T completionStatus) {
        this.completionStatus = completionStatus;
    }

    public T getCategory() {
        return category;
    }

    public void setCategory(T category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "Task{" +
                "taskId=" + taskId +
                ", taskName=" + taskName +
                ", description=" + description +
                ", completionStatus=" + completionStatus +
                ", category=" + category +
                '}';
    }
}
