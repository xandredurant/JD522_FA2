/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.jd522_fa2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.table.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 *
 * @author Xandr
 */
public class TaskManagerApp extends javax.swing.JFrame {

    /**
     * Creates new form TaskManagerApp
     */
    DefaultTableModel model;
    private List<Task<String>> tasks = new ArrayList<>();
    private Map<String, List<Task<String>>> taskCategories = new HashMap<>();
    private TaskEntryHandler taskEntryHandler;
    
    // Database connection method
    private Connection getConnection() throws SQLException {
        // Retrieve database connection details from environment variables
        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        return DriverManager.getConnection(url, username, password);
    }
    
    // Method to get the maximum task ID from the database
    private int getMaxTaskId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT MAX(task_id) FROM tasks");
             ResultSet resultSet = statement.executeQuery();) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 1; // Return 1 if no tasks are present in the table
        }
    }
    
    // Initialize the table with data from the database
    private void initializeTable() {
        model = (DefaultTableModel) jTable1.getModel();
        JTable jTable = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        jTable1.setRowSorter(sorter);
        
        model.setRowCount(0); // Clear existing dat
        
        // Retrieve tasks from the database and populate the table
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM tasks");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                // Extract task details from the database
                int taskID = resultSet.getInt("task_id");
                String taskName = resultSet.getString("task_name");
                String description = resultSet.getString("description");
                String completionStatus = resultSet.getString("completion_status");
                String category = resultSet.getString("category");
                
                Task<String> task = new Task<>(taskID, taskName, description, completionStatus, category);
                tasks.add(task);
                // Add the task to its category
                addTaskToCategory(task.getCategory(),task);
                // Add the task to the table model
                model.addRow(new Object[]{taskID, taskName, description, completionStatus, category});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle any database-related errors here
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    // Inner class for handling GUI components for task entry
    private class TaskEntryHandler {
        private JTextField taskNameField;
        private JTextArea descriptionArea;
        private JComboBox<String> completionStatusComboBox;
        private JComboBox<String> categoryComboBox;

        public TaskEntryHandler(JTextField taskNameField, JTextArea descriptionArea, JComboBox<String> completionStatusComboBox, JComboBox<String> categoryComboBox) {
            this.taskNameField = taskNameField;
            this.descriptionArea = descriptionArea;
            this.completionStatusComboBox = completionStatusComboBox;
            this.categoryComboBox = categoryComboBox;
        }

        public String getTaskName() {
            return taskNameField.getText();
        }

        public String getDescription() {
            return descriptionArea.getText();
        }

        public String getCompletionStatus() {
            return completionStatusComboBox.getSelectedItem().toString();
        }

        public String getCategory() {
            return categoryComboBox.getSelectedItem().toString();
        }
        
        public int getCompletionStatusIndex() {
            return completionStatusComboBox.getSelectedIndex();
        }
        
        public int getCategoryIndex() {
            return categoryComboBox.getSelectedIndex();
        }
    }
    
    // Inner class to manage task categories and their corresponding actions
    private class CategoryManager {
        private JComboBox<String> categoryChoiceComboBox;
        private JComboBox<String> categoryActionComboBox;
        private JComboBox<String> filterCategoryComboBox;
        private JTextField categoryNameField;
        private JButton submitCategoryButton;

        public CategoryManager(JComboBox<String> categoryChoiceComboBox, JComboBox<String> filterCategoryComboBox, JComboBox<String> categoryActionComboBox, JTextField categoryNameField, JButton submitCategoryButton) {
            this.categoryChoiceComboBox = categoryChoiceComboBox;
            this.categoryActionComboBox = categoryActionComboBox;
            this.categoryNameField = categoryNameField;
            this.submitCategoryButton = submitCategoryButton;
            this.filterCategoryComboBox = filterCategoryComboBox;

            loadCategories(categoryChoiceComboBox, filterCategoryComboBox);
            categoryChoiceComboBox.setSelectedIndex(-1);
            
            submitCategoryButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedAction = categoryActionComboBox.getSelectedItem().toString();
                    String categoryName = categoryNameField.getText();

                    if (selectedAction.equals("Add")) {
                        // Handle adding a new category
                        if (!categoryName.isEmpty()) {
                            addCategory(categoryName);
                            // Add the category to the combo box or database
                            categoryChoiceComboBox.addItem(categoryName);
                            filterCategoryComboBox.addItem(categoryName);
                        }
                    } else if (selectedAction.equals("Delete")) {
                        deleteCategory(categoryName);
                        // Handle deleting an existing category
                        // Remove the category from the combo box or database
                        categoryChoiceComboBox.removeItem(categoryName);
                        filterCategoryComboBox.removeItem(categoryName);
                    }
                }
            });
        }
        //Method to load the categories
        private void loadCategories(JComboBox<String> categoryChoiceComboBox, JComboBox<String> filterCategoryComboBox) {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT Category_name FROM Categories");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String category = resultSet.getString("Category_name");
                    categoryChoiceComboBox.addItem(category);
                    filterCategoryComboBox.addItem(category);

                    if (!taskCategories.containsKey(category)) {
                        taskCategories.put(category, new ArrayList<>());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Handle any database-related errors here
            }
        }
        //Method to add new category
        public boolean addCategory(String categoryName) {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO Categories (Category_name) VALUES (?)")) {

                statement.setString(1, categoryName);
                int rowsInserted = statement.executeUpdate();

                if (!taskCategories.containsKey(categoryName)) {
                    taskCategories.put(categoryName, new ArrayList<>());
                }

                return rowsInserted > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                // Handle any database-related errors here
                return false;
            }
        }
        //Method to delete a category
        public boolean deleteCategory(String categoryName) {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM Categories WHERE Category_name = ?")) {

                statement.setString(1, categoryName);
                int rowsDeleted = statement.executeUpdate();

                return rowsDeleted > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                // Handle any database-related errors here
                return false;
            }
        }
    }
    //Method to add a task to a category in the hashmap
    public boolean addTaskToCategory(String categoryName, Task<String> task) {
        List<Task<String>> categoryTasks = taskCategories.get(categoryName);
        if (categoryTasks != null) {
            categoryTasks.add(task);
            return true; // Return true if the task was added to the category successfully
        }
        return false; // Return false if the category does not exist
    }
    //Method to update the category of a task in the hashmap
    private void updateCategoryForTask(int selectedIndex, String newCategory) {
        Task<String> updatedTask = tasks.get(selectedIndex);
        String oldCategory = updatedTask.getCategory();

        if (!oldCategory.equals("N/A")) {
            // Remove the task from the old category
            List<Task<String>> oldCategoryTasks = taskCategories.get(oldCategory);
            oldCategoryTasks.remove(updatedTask);
        }

        // Update the task's category
        updatedTask.setCategory(newCategory);

        // Add the task to the new category
        if (!newCategory.equals("N/A")) {
            taskCategories.computeIfAbsent(newCategory, k -> new ArrayList<>()).add(updatedTask);
        }
    }
    //Method to remove a task from a category in the hashmap
    private void removeTaskFromCategory(int selectedIndex) {
        Task<String> removedTask = tasks.get(selectedIndex);
        String category = removedTask.getCategory();

        if (!category.equals("N/A")) {
            List<Task<String>> categoryTasks = taskCategories.get(category);

            // Check if the task exists in the category before removing it
            if (categoryTasks != null && categoryTasks.contains(removedTask)) {
                categoryTasks.remove(removedTask);
            }
        }
    }
    //Method to clear inputs
    private void clearInputFields() {
        tf_Name.setText("");
        ta_description.setText("");
    }
    //Method to save the tasks to a text file
    private File saveTasksToFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = "TaskManager_save_" + timeStamp + ".txt"; // Include timestamp in the filename
        File file = new File(fileName);

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(tasks);
            return file; // Return the saved File

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    //Method to load the tasks from a text file
    private void loadTasksFromFile(String filename) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(filename))) {
            ArrayList<Task<String>> loadedTasks = (ArrayList<Task<String>>) inputStream.readObject();

            // Append the loaded tasks to the existing ArrayList
            tasks.addAll(loadedTasks);

            // Insert the loaded tasks into the database
            try (Connection connection = getConnection()) {
                for (Task<String> task : loadedTasks) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO tasks (task_name, description, completion_status, category) VALUES (?, ?, ?, ?)")) {
                        statement.setString(1, task.getTaskName());
                        statement.setString(2, task.getDescription());
                        statement.setString(3, task.getCompletionStatus());
                        statement.setString(4, task.getCategory());

                        statement.executeUpdate();
                    }
                }
            }

            // Update the table model with the loaded tasks
            for (Task<String> task : loadedTasks) {
                model.addRow(new Object[]{task.getTaskId(), task.getTaskName(), task.getDescription(), task.getCompletionStatus(), task.getCategory()});
                addTaskToCategory(task.getCategory(),task);
            }

            JOptionPane.showMessageDialog(this, "Tasks loaded from " + filename, "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading tasks from " + filename, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to display file properties
    private void displayFileProperties(File file) {
        if (file != null && file.exists()) {
            String fileName = file.getName();
            l_fileName.setText("File Name: " + fileName);

            try {
                BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                long fileSize = attributes.size();
                l_fileSize.setText("File Size: " + fileSize + " bytes");

                FileTime creationTime = attributes.creationTime();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                String formattedCreationTime = sdf.format(new Date(creationTime.toMillis()));
                l_fileCreationD.setText("File Creation Date: " + formattedCreationTime);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle the case when the file doesn't exist
            l_fileName.setText("File Name: N/A");
            l_fileSize.setText("File Size: N/A");
            l_fileCreationD.setText("File Creation Date: N/A");
        }
    }
    // Method to export tasks to a CSV file
    private void exportTasksToCSV(String filename) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            // Write CSV headers
            csvPrinter.printRecord("Task ID", "Task Name", "Description", "Completion Status", "Category");

            // Write task data to CSV
            for (Task<String> task : tasks) {
                csvPrinter.printRecord(task.getTaskId(), task.getTaskName(), task.getDescription(), task.getCompletionStatus(), task.getCategory());
            }

            JOptionPane.showMessageDialog(this, "Tasks exported to CSV file " + filename, "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting tasks to " + filename, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    // Method to apply table filters
    private void applyFilters() {
        String filterName = tf_filterName.getText();
        String filterCS = cb_filterCS.getSelectedItem().toString();
        String filterCategory = cb_filterCategory.getSelectedItem().toString();

        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) jTable1.getRowSorter();
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        sorter.setRowFilter(null);
        
        if (!filterName.isEmpty()) {
        filters.add(RowFilter.regexFilter("(?i)^" + Pattern.quote(filterName) + "$", 1));
        }

        if (!filterCS.equals("Do not specify")) {
            filters.add(RowFilter.regexFilter("(?i)^" + Pattern.quote(filterCS) + "$", 3));
        }

        if (!filterCategory.equals("Do not specify")) {
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    String taskName = (String) entry.getValue(1);
                    
                    List<Task<String>> categoryTasks = taskCategories.get(filterCategory);
                    if (categoryTasks != null) {
                        for (Task<String> task : categoryTasks) {
                            if (task.getTaskName().equalsIgnoreCase(taskName)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }

        if (!filters.isEmpty()) {
            RowFilter<Object, Object> combinedFilter = RowFilter.andFilter(filters);
            sorter.setRowFilter(combinedFilter);
        } else {
            sorter.setRowFilter(null);
        }
        
    }
    // Custom combo box renderer for consistent UI
    public class CustomComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // Set the text color to white for all items
            component.setForeground(Color.WHITE);
            component.setBackground(Color.BLACK);
            
            return component;
        }
    }
    // Custom table cell renderer for consistent UI
    class CustomTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Set row background color
            if (row % 2 == 0) {
                c.setBackground(Color.BLACK);
            } else {
                c.setBackground(Color.DARK_GRAY);
            }

            // Set row foreground color
            c.setForeground(Color.WHITE);
            
            ((JComponent) c).setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.WHITE));
            
            return c;
        }
    }
    // Custom table header renderer for consistent UI
    class CustomTableHeaderRenderer extends DefaultTableCellRenderer {
        public CustomTableHeaderRenderer() {
            setHorizontalAlignment(JLabel.CENTER); // Center-align the header text
            setForeground(Color.WHITE); // Set the text color to white
        }
        
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Set column header background color
            c.setBackground(Color.BLACK);

            // Set column header foreground color
            c.setForeground(Color.WHITE);
            
            ((JComponent) c).setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.WHITE));
            
            return c;
        }
    }
    // Constructor for the TaskManagerApp class
    public TaskManagerApp() {
        initComponents();
        
        taskEntryHandler = new TaskEntryHandler(tf_Name, ta_description, cb_CompleteS, cb_category);
        CategoryManager categoryManager = new CategoryManager(cb_category,cb_filterCategory,cb_categoryAction,tf_categoryName, b_submitCategory);
        
        initializeTable();
        // Set custom renderers for combo boxes, table cells, and headers
        cb_category.setRenderer(new CustomComboBoxRenderer());
        cb_CompleteS.setRenderer(new CustomComboBoxRenderer());
        cb_categoryAction.setRenderer(new CustomComboBoxRenderer());
        cb_filterCS.setRenderer(new CustomComboBoxRenderer());
        cb_filterCategory.setRenderer(new CustomComboBoxRenderer());
        
        // Create and set the custom row and table background renderer
        jTable1.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

        // Customize the column header colors
        jTable1.getTableHeader().setDefaultRenderer(new CustomTableHeaderRenderer());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        tf_Name = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        ta_description = new javax.swing.JTextArea();
        cb_CompleteS = new javax.swing.JComboBox<>();
        cb_category = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        b_add = new javax.swing.JButton();
        b_edit = new javax.swing.JButton();
        b_delete = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        cb_categoryAction = new javax.swing.JComboBox<>();
        tf_categoryName = new javax.swing.JTextField();
        b_submitCategory = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        b_load = new javax.swing.JButton();
        b_save = new javax.swing.JButton();
        b_export = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        l_fileName = new javax.swing.JLabel();
        l_fileSize = new javax.swing.JLabel();
        l_fileCreationD = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        cb_filterCS = new javax.swing.JComboBox<>();
        tf_filterName = new javax.swing.JTextField();
        b_filter = new javax.swing.JButton();
        cb_filterCategory = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(0, 0, 0));

        jPanel1.setBackground(new java.awt.Color(102, 102, 102));

        jPanel2.setBackground(new java.awt.Color(0, 0, 0));

        tf_Name.setBackground(new java.awt.Color(0, 0, 0));
        tf_Name.setForeground(new java.awt.Color(255, 255, 255));

        ta_description.setBackground(new java.awt.Color(0, 0, 0));
        ta_description.setColumns(20);
        ta_description.setForeground(new java.awt.Color(255, 255, 255));
        ta_description.setLineWrap(true);
        ta_description.setRows(2);
        ta_description.setWrapStyleWord(true);
        jScrollPane2.setViewportView(ta_description);

        cb_CompleteS.setBackground(new java.awt.Color(0, 0, 0));
        cb_CompleteS.setForeground(new java.awt.Color(255, 255, 255));
        cb_CompleteS.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Not Started", "In Progress", "Completed", "On Hold", "Cancelled" }));
        cb_CompleteS.setSelectedIndex(-1);
        cb_CompleteS.setToolTipText("");

        cb_category.setBackground(new java.awt.Color(0, 0, 0));
        cb_category.setForeground(new java.awt.Color(255, 255, 255));

        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Name:");

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Description:");

        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Completion status:");

        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Category:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(tf_Name, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cb_category, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cb_CompleteS, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_Name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cb_CompleteS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(cb_category, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(0, 0, 0));

        b_add.setBackground(new java.awt.Color(0, 0, 0));
        b_add.setForeground(new java.awt.Color(255, 255, 255));
        b_add.setText("Add");
        b_add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_addActionPerformed(evt);
            }
        });

        b_edit.setBackground(new java.awt.Color(0, 0, 0));
        b_edit.setForeground(new java.awt.Color(255, 255, 255));
        b_edit.setText("Edit");
        b_edit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_editActionPerformed(evt);
            }
        });

        b_delete.setBackground(new java.awt.Color(0, 0, 0));
        b_delete.setForeground(new java.awt.Color(255, 255, 255));
        b_delete.setText("Delete");
        b_delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_deleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(b_add)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                .addComponent(b_edit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addComponent(b_delete)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(b_add)
                    .addComponent(b_edit)
                    .addComponent(b_delete))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel8.setBackground(new java.awt.Color(0, 0, 0));

        cb_categoryAction.setBackground(new java.awt.Color(0, 0, 0));
        cb_categoryAction.setForeground(new java.awt.Color(255, 255, 255));
        cb_categoryAction.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Add", "Delete" }));

        tf_categoryName.setBackground(new java.awt.Color(0, 0, 0));
        tf_categoryName.setForeground(new java.awt.Color(255, 255, 255));

        b_submitCategory.setBackground(new java.awt.Color(0, 0, 0));
        b_submitCategory.setForeground(new java.awt.Color(255, 255, 255));
        b_submitCategory.setText("Confirm");

        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Create or delete categories:");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cb_categoryAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(b_submitCategory)
                    .addComponent(tf_categoryName, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cb_categoryAction, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(tf_categoryName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(b_submitCategory)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(102, 102, 102));

        jTable1.setBackground(new java.awt.Color(0, 0, 0));
        jTable1.setForeground(new java.awt.Color(255, 255, 255));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Task ID", "Name", "Description", "Status", "Category"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.setEnabled(false);
        jTable1.setFocusable(false);
        jTable1.setGridColor(new java.awt.Color(255, 255, 255));
        jScrollPane1.setViewportView(jTable1);

        jPanel5.setBackground(new java.awt.Color(0, 0, 0));

        b_load.setBackground(new java.awt.Color(0, 0, 0));
        b_load.setForeground(new java.awt.Color(255, 255, 255));
        b_load.setText("Load");
        b_load.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_loadActionPerformed(evt);
            }
        });

        b_save.setBackground(new java.awt.Color(0, 0, 0));
        b_save.setForeground(new java.awt.Color(255, 255, 255));
        b_save.setText("Save");
        b_save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_saveActionPerformed(evt);
            }
        });

        b_export.setBackground(new java.awt.Color(0, 0, 0));
        b_export.setForeground(new java.awt.Color(255, 255, 255));
        b_export.setText("Export");
        b_export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_exportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(b_load)
                .addGap(131, 131, 131)
                .addComponent(b_save)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(b_export)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(b_load)
                    .addComponent(b_save)
                    .addComponent(b_export))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBackground(new java.awt.Color(0, 0, 0));

        l_fileName.setForeground(new java.awt.Color(255, 255, 255));
        l_fileName.setText("File name");

        l_fileSize.setForeground(new java.awt.Color(255, 255, 255));
        l_fileSize.setText("File Size");

        l_fileCreationD.setForeground(new java.awt.Color(255, 255, 255));
        l_fileCreationD.setText("File Creation date");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(l_fileName)
                    .addComponent(l_fileSize)
                    .addComponent(l_fileCreationD))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(l_fileName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(l_fileSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(l_fileCreationD)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel7.setBackground(new java.awt.Color(0, 0, 0));

        cb_filterCS.setBackground(new java.awt.Color(0, 0, 0));
        cb_filterCS.setForeground(new java.awt.Color(255, 255, 255));
        cb_filterCS.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Do not specify", "Not Started", "In Progress", "Completed", "On Hold", "Cancelled" }));

        tf_filterName.setBackground(new java.awt.Color(0, 0, 0));
        tf_filterName.setForeground(new java.awt.Color(255, 255, 255));
        tf_filterName.setToolTipText("Task Name");

        b_filter.setBackground(new java.awt.Color(0, 0, 0));
        b_filter.setForeground(new java.awt.Color(255, 255, 255));
        b_filter.setText("Filter");
        b_filter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_filterActionPerformed(evt);
            }
        });

        cb_filterCategory.setBackground(new java.awt.Color(0, 0, 0));
        cb_filterCategory.setForeground(new java.awt.Color(255, 255, 255));
        cb_filterCategory.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Do not specify" }));

        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Filter tasks displayed:");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(tf_filterName, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(44, 44, 44)
                        .addComponent(cb_filterCS, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 44, Short.MAX_VALUE)
                        .addComponent(cb_filterCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(b_filter)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(b_filter)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_filterName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cb_filterCS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cb_filterCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Action performed when "Add" button is clicked
    private void b_addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_addActionPerformed
        // Retrieve task details from input fields
        String taskName = taskEntryHandler.getTaskName();
        String description = taskEntryHandler.getDescription();
        String completionStatus = "N/A";
        String category = "N/A";
        boolean validFields = true;
        
        // Validate task input fields
        if (taskName.isEmpty()){
            validFields = false;
            JOptionPane.showMessageDialog(this, "Task Name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        }else if (taskEntryHandler.getCompletionStatusIndex() == -1){
            validFields = false;
            JOptionPane.showMessageDialog(this, "Select completion status.", "Error", JOptionPane.ERROR_MESSAGE);
        }else if(taskEntryHandler.getCategoryIndex() == -1){
             validFields = false;
            JOptionPane.showMessageDialog(this, "Select category.", "Error", JOptionPane.ERROR_MESSAGE);
        }else{
            // Update completionStatus and category if valid
            completionStatus = taskEntryHandler.getCompletionStatus();
            category = taskEntryHandler.getCategory();
        }
        
        // If all fields are valid, insert the task into the database
        if (validFields) {
            try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO tasks (task_name, description, completion_status, category) VALUES (?, ?, ?, ?)")) {
                
                statement.setString(1, taskName);
                statement.setString(2, description);
                statement.setString(3, completionStatus);
                statement.setString(4, category);

                int rowsInserted = statement.executeUpdate();
                
                // Task inserted successfully into the database
                if (rowsInserted > 0) {
                    // Task inserted successfully into the database
                    int taskID = getMaxTaskId(connection);

                    // Add the task to your ArrayList and update the table model
                    Task<String> task = new Task<>(taskID, taskName, description, completionStatus, category);
                    tasks.add(task);
                    addTaskToCategory(task.getCategory(),task);
                    
                    DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                    model.addRow(new Object[]{taskID, taskName, description, completionStatus, category});

                    // Clear the input fields
                    clearInputFields();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Handle any database-related errors here
            }
        }
    }//GEN-LAST:event_b_addActionPerformed
    // Add the task to your ArrayList and update the table model
    private void b_editActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_editActionPerformed
        // Retrieve task details from input fields
        String taskName = taskEntryHandler.getTaskName();
        String description = taskEntryHandler.getDescription();
        String completionStatus = "N/A";
        String category = "N/A";
        boolean validFields = true;
        
        // Validate task input fields
        if (taskName.isEmpty()){
            validFields = false;
            JOptionPane.showMessageDialog(this, "Task Name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        }else if (taskEntryHandler.getCompletionStatusIndex() == -1){
            validFields = false;
            JOptionPane.showMessageDialog(this, "Select completion status.", "Error", JOptionPane.ERROR_MESSAGE);
        }else if(taskEntryHandler.getCategoryIndex() == -1){
             validFields = false;
            JOptionPane.showMessageDialog(this, "Select category.", "Error", JOptionPane.ERROR_MESSAGE);
        }else{
            // Update completionStatus and category if valid
            completionStatus = taskEntryHandler.getCompletionStatus();
            category = taskEntryHandler.getCategory();
        }

        // Ensure that the fields is not empty before proceeding
        if (validFields) {
            // Find the index of the task with the same taskName
            int selectedIndex = -1;
            for (int i = 0; i < tasks.size(); i++) {
                Task<String> task = tasks.get(i);
                if (task.getTaskName().equals(taskName)) {
                    selectedIndex = i;
                    break;
                }
            }

            if (selectedIndex != -1) {
                // Update the task in the database
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "UPDATE tasks SET task_name=?, description=?, completion_status=?, category=? WHERE task_id=?")) {
                    statement.setString(1, taskName);
                    statement.setString(2, description);
                    statement.setString(3, completionStatus);
                    statement.setString(4, category);
                    statement.setInt(5, tasks.get(selectedIndex).getTaskId()); // Use the task_id

                    int rowsUpdated = statement.executeUpdate();

                    if (rowsUpdated > 0) {
                        // Update the task in your ArrayList and the table model
                        if (!tasks.get(selectedIndex).getCategory().equals(category)) {
                            updateCategoryForTask(selectedIndex, category);
                        }
                        
                        // Task updated successfully in the database
                        // Update the task in the ArrayList and the table model
                        Task<String> updatedTask = new Task<>(tasks.get(selectedIndex).getTaskId(), taskName, description, completionStatus, category);
                        tasks.set(selectedIndex, updatedTask);

                        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                        model.setValueAt(taskName, selectedIndex, 1);
                        model.setValueAt(description, selectedIndex, 2);
                        model.setValueAt(completionStatus, selectedIndex, 3);
                        model.setValueAt(category, selectedIndex, 4);

                        // Clear the input fields
                        clearInputFields();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    // Handle any database-related errors here
                }
            } else {
                JOptionPane.showMessageDialog(this, "Task with the specified name not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } 
    }//GEN-LAST:event_b_editActionPerformed
    // Action performed when "Delete" button is clicked
    private void b_deleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_deleteActionPerformed
        String taskName = taskEntryHandler.getTaskName();
    
        // Ensure that the taskName is not empty before proceeding
        if (!taskName.isEmpty()) {
            
            // Find the index of the task with the same taskName
            int selectedIndex = -1;
            for (int i = 0; i < tasks.size(); i++) {
                Task<String> task = tasks.get(i);
                if (task.getTaskName().equals(taskName)) {
                    selectedIndex = i;
                    break;
                }
            }
            
            if (selectedIndex != -1) {
                // Delete the task from the database
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM tasks WHERE task_id=?")) {
                    statement.setInt(1, tasks.get(selectedIndex).getTaskId()); // Use the task_id
                    
                    int rowsDeleted = statement.executeUpdate();

                    if (rowsDeleted > 0) {
                        // Task deleted successfully from the database
                        // Remove the task from your ArrayList and the table model
                        tasks.remove(selectedIndex);

                        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                        model.removeRow(selectedIndex);
                        
                        // Clear the input fields
                        clearInputFields();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    // Handle any database-related errors here
                }
            } else {
                JOptionPane.showMessageDialog(this, "Task with the specified name not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Task Name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_b_deleteActionPerformed
    // Action performed when "Filter" button is clicked
    private void b_filterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_filterActionPerformed
        applyFilters();
    }//GEN-LAST:event_b_filterActionPerformed
    // Action performed when "Save" button is clicked
    private void b_saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_saveActionPerformed
        File savedFile = saveTasksToFile();
        displayFileProperties(savedFile);
    }//GEN-LAST:event_b_saveActionPerformed
    // Action performed when "Load" button is clicked
    private void b_loadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_loadActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadTasksFromFile(selectedFile.getAbsolutePath());
            displayFileProperties(selectedFile);
        }
    }//GEN-LAST:event_b_loadActionPerformed
    // Action performed when "Export" button is clicked
    private void b_exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_exportActionPerformed
        exportTasksToCSV("TaskManager_CSV.csv");
    }//GEN-LAST:event_b_exportActionPerformed
    /**
     * @param args the command line arguments
     */
    // Main method to launch the application
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TaskManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TaskManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TaskManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TaskManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                TaskManagerApp app = new TaskManagerApp();
                app.setVisible(true);
                app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton b_add;
    private javax.swing.JButton b_delete;
    private javax.swing.JButton b_edit;
    private javax.swing.JButton b_export;
    private javax.swing.JButton b_filter;
    private javax.swing.JButton b_load;
    private javax.swing.JButton b_save;
    private javax.swing.JButton b_submitCategory;
    private javax.swing.JComboBox<String> cb_CompleteS;
    private javax.swing.JComboBox<String> cb_category;
    private javax.swing.JComboBox<String> cb_categoryAction;
    private javax.swing.JComboBox<String> cb_filterCS;
    private javax.swing.JComboBox<String> cb_filterCategory;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel l_fileCreationD;
    private javax.swing.JLabel l_fileName;
    private javax.swing.JLabel l_fileSize;
    private javax.swing.JTextArea ta_description;
    private javax.swing.JTextField tf_Name;
    private javax.swing.JTextField tf_categoryName;
    private javax.swing.JTextField tf_filterName;
    // End of variables declaration//GEN-END:variables
}
