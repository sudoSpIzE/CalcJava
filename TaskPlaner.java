import java.util.*;
import java.time.*;
import java.time.format.*;
import java.io.*;

public class TaskPlanner {

    // Перечисления для статуса и приоритета
    enum Status {
        TODO("К выполнению"),
        IN_PROGRESS("В процессе"),
        DONE("Выполнено"),
        CANCELLED("Отменено");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    enum Priority {
        HIGH("🔴 Высокий"),
        MEDIUM("🟡 Средний"),
        LOW("🟢 Низкий");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Класс задачи
    static class Task {
        private int id;
        private String title;
        private String description;
        private Status status;
        private Priority priority;
        private LocalDate deadline;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Task(int id, String title, String description,
                    Status status, Priority priority, LocalDate deadline,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.status = status;
            this.priority = priority;
            this.deadline = deadline;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Task(int id, String title, String description,
                    Status status, Priority priority, LocalDate deadline) {
            this(id, title, description, status, priority, deadline,
                    LocalDateTime.now(), LocalDateTime.now());
        }

        // Геттеры
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Status getStatus() { return status; }
        public Priority getPriority() { return priority; }
        public LocalDate getDeadline() { return deadline; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }

        // Сеттеры
        public void setTitle(String title) {
            this.title = title;
            this.updatedAt = LocalDateTime.now();
        }

        public void setDescription(String description) {
            this.description = description;
            this.updatedAt = LocalDateTime.now();
        }

        public void setStatus(Status status) {
            this.status = status;
            this.updatedAt = LocalDateTime.now();
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
            this.updatedAt = LocalDateTime.now();
        }

        public void setDeadline(LocalDate deadline) {
            this.deadline = deadline;
            this.updatedAt = LocalDateTime.now();
        }

        // Проверка просроченности
        public boolean isOverdue() {
            return deadline != null && deadline.isBefore(LocalDate.now()) && status != Status.DONE;
        }

        // Дней до дедлайна
        public long daysUntilDeadline() {
            if (deadline == null) return Long.MAX_VALUE;
            return deadline.toEpochDay() - LocalDate.now().toEpochDay();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("┌─────────────────────────────────────────────────\n");
            sb.append(String.format("│ ID: %d\n", id));
            sb.append(String.format("│ 📌 %s\n", title));
            sb.append(String.format("│ 📝 %s\n", description.isEmpty() ? "(без описания)" : description));
            sb.append(String.format("│ 🏷️  Статус: %s\n", status));
            sb.append(String.format("│ ⚡ Приоритет: %s\n", priority));

            if (deadline != null) {
                String deadlineStr = deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                if (isOverdue()) {
                    sb.append(String.format("│ ⏰ Дедлайн: %s (❗ПРОСРОЧЕНО❗)\n", deadlineStr));
                } else if (daysUntilDeadline() <= 3) {
                    sb.append(String.format("│ ⏰ Дедлайн: %s (⚠️ СКОРО истекает: %d дней)\n",
                            deadlineStr, daysUntilDeadline()));
                } else {
                    sb.append(String.format("│ ⏰ Дедлайн: %s (осталось %d дней)\n",
                            deadlineStr, daysUntilDeadline()));
                }
            } else {
                sb.append("│ ⏰ Дедлайн: не установлен\n");
            }

            sb.append(String.format("│ 📅 Создано: %s\n",
                    createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
            sb.append(String.format("│ 🔄 Обновлено: %s\n",
                    updatedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
            sb.append("└─────────────────────────────────────────────────");

            return sb.toString();
        }

        // Для CSV экспорта
        public String toCSV() {
            return String.format("%d;%s;%s;%s;%s;%s;%s;%s",
                    id,
                    title.replace(";", ","),
                    description.replace(";", ","),
                    status.name(),
                    priority.name(),
                    deadline != null ? deadline.format(DateTimeFormatter.ISO_DATE) : "",
                    createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
        }

        // Для JSON экспорта
        public String toJSON() {
            return String.format("""
                {
                  "id": %d,
                  "title": "%s",
                  "description": "%s",
                  "status": "%s",
                  "priority": "%s",
                  "deadline": "%s",
                  "createdAt": "%s",
                  "updatedAt": "%s"
                }""",
                    id,
                    escapeJson(title),
                    escapeJson(description),
                    status.name(),
                    priority.name(),
                    deadline != null ? deadline.format(DateTimeFormatter.ISO_DATE) : "",
                    createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
        }

        private String escapeJson(String str) {
            return str.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    // Основной класс приложения
    private List<Task> tasks;
    private int nextId;
    private final Scanner scanner;
    private static final String CSV_FILE = "tasks.csv";
    private static final String JSON_FILE = "tasks.json";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public TaskPlanner() {
        tasks = new ArrayList<>();
        scanner = new Scanner(System.in);
        nextId = 1;
        loadFromCSV();
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    public void run() {
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("        📝 ПЛАНИРОВЩИК ЗАДАЧ v1.0");
        System.out.println("═══════════════════════════════════════════════");

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addTask();
                case "2" -> showAllTasks();
                case "3" -> editTask();
                case "4" -> deleteTask();
                case "5" -> filterTasks();
                case "6" -> searchTasks();
                case "7" -> showStatistics();
                case "8" -> showUpcomingTasks();
                case "9" -> saveToCSV();
                case "10" -> saveToJSON();
                case "11" -> loadFromCSV();
                case "12" -> loadFromJSON();
                case "0" -> {
                    saveToCSV();
                    System.out.println("👋 До свидания! Все данные сохранены.");
                    return;
                }
                default -> System.out.println("❌ Неверный выбор. Попробуйте снова.");
            }

            System.out.println("\nНажмите Enter для продолжения...");
            scanner.nextLine();
        }
    }

    private void printMenu() {
        clearScreen();
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("                  ГЛАВНОЕ МЕНЮ");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("1. ➕ Добавить новую задачу");
        System.out.println("2. 👁️  Показать все задачи");
        System.out.println("3. ✏️  Редактировать задачу");
        System.out.println("4. ❌ Удалить задачу");
        System.out.println("5. 🔍 Фильтровать задачи");
        System.out.println("6. 🔎 Поиск задач");
        System.out.println("7. 📊 Статистика");
        System.out.println("8. ⏰ Предстоящие задачи");
        System.out.println("9. 💾 Сохранить в CSV");
        System.out.println("10. 💾 Сохранить в JSON");
        System.out.println("11. 📂 Загрузить из CSV");
        System.out.println("12. 📂 Загрузить из JSON");
        System.out.println("0. 🚪 Выход");
        System.out.println("═══════════════════════════════════════════════");
        System.out.print("Выберите действие: ");
    }

    // ==================== ОПЕРАЦИИ С ЗАДАЧАМИ ====================

    private void addTask() {
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("              ДОБАВЛЕНИЕ НОВОЙ ЗАДАЧИ");
        System.out.println("═══════════════════════════════════════════════");

        System.out.print("📌 Название задачи: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("❌ Название не может быть пустым!");
            return;
        }

        System.out.print("📝 Описание (Enter чтобы пропустить): ");
        String description = scanner.nextLine().trim();

        Status status = selectStatus();
        Priority priority = selectPriority();
        LocalDate deadline = selectDeadline();

        Task task = new Task(nextId++, title, description, status, priority, deadline);
        tasks.add(task);

        System.out.println("\n✅ Задача успешно добавлена!");
        System.out.println(task);
    }

    private void showAllTasks() {
        if (tasks.isEmpty()) {
            System.out.println("\n📭 Список задач пуст.");
            return;
        }

        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("               ВСЕ ЗАДАЧИ (" + tasks.size() + ")");
        System.out.println("═══════════════════════════════════════════════");

        List<Task> sortedTasks = new ArrayList<>(tasks);
        sortedTasks.sort((t1, t2) -> {
            // Сортировка по приоритету
            int priorityCompare = Integer.compare(
                    getPriorityValue(t1.getPriority()),
                    getPriorityValue(t2.getPriority())
            );
            if (priorityCompare != 0) return priorityCompare;

            // Сортировка по дедлайну
            LocalDate d1 = t1.getDeadline() != null ? t1.getDeadline() : LocalDate.MAX;
            LocalDate d2 = t2.getDeadline() != null ? t2.getDeadline() : LocalDate.MAX;
            int dateCompare = d1.compareTo(d2);
            if (dateCompare != 0) return dateCompare;

            // Сортировка по ID
            return Integer.compare(t1.getId(), t2.getId());
        });

        for (Task task : sortedTasks) {
            System.out.println(task);
            System.out.println();
        }

        long overdueCount = 0;
        for (Task task : tasks) {
            if (task.isOverdue()) {
                overdueCount++;
            }
        }

        if (overdueCount > 0) {
            System.out.println("⚠️  ВНИМАНИЕ: " + overdueCount + " задач просрочено!");
        }
    }

    private int getPriorityValue(Priority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    private void editTask() {
        if (tasks.isEmpty()) {
            System.out.println("\n📭 Нет задач для редактирования.");
            return;
        }

        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("               РЕДАКТИРОВАНИЕ ЗАДАЧИ");
        System.out.println("═══════════════════════════════════════════════");

        showAllTasks();
        System.out.print("\nВведите ID задачи для редактирования: ");

        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            Task task = findTaskById(id);

            if (task == null) {
                System.out.println("❌ Задача с ID " + id + " не найдена.");
                return;
            }

            System.out.println("\nРедактирование задачи:");
            System.out.println(task);

            System.out.println("\nЧто вы хотите изменить?");
            System.out.println("1. 📌 Название");
            System.out.println("2. 📝 Описание");
            System.out.println("3. 🏷️  Статус");
            System.out.println("4. ⚡ Приоритет");
            System.out.println("5. ⏰ Дедлайн");
            System.out.println("6. ✏️  Все поля");
            System.out.println("0. ↩️  Отмена");
            System.out.print("Выберите: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Новое название: ");
                    String newTitle = scanner.nextLine().trim();
                    if (!newTitle.isEmpty()) {
                        task.setTitle(newTitle);
                    }
                }
                case "2" -> {
                    System.out.print("Новое описание: ");
                    task.setDescription(scanner.nextLine().trim());
                }
                case "3" -> {
                    Status newStatus = selectStatus();
                    task.setStatus(newStatus);
                }
                case "4" -> {
                    Priority newPriority = selectPriority();
                    task.setPriority(newPriority);
                }
                case "5" -> {
                    LocalDate newDeadline = selectDeadline();
                    task.setDeadline(newDeadline);
                }
                case "6" -> {
                    System.out.print("Новое название: ");
                    String newTitle = scanner.nextLine().trim();
                    if (!newTitle.isEmpty()) {
                        task.setTitle(newTitle);
                    }

                    System.out.print("Новое описание: ");
                    task.setDescription(scanner.nextLine().trim());

                    task.setStatus(selectStatus());
                    task.setPriority(selectPriority());
                    task.setDeadline(selectDeadline());
                }
                case "0" -> {
                    System.out.println("✖️ Редактирование отменено.");
                    return;
                }
                default -> {
                    System.out.println("❌ Неверный выбор.");
                    return;
                }
            }

            System.out.println("\n✅ Задача успешно обновлена!");
            System.out.println(task);

        } catch (NumberFormatException e) {
            System.out.println("❌ Неверный формат ID.");
        }
    }

    private void deleteTask() {
        if (tasks.isEmpty()) {
            System.out.println("\n📭 Нет задач для удаления.");
            return;
        }

        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("                 УДАЛЕНИЕ ЗАДАЧИ");
        System.out.println("═══════════════════════════════════════════════");

        showAllTasks();
        System.out.print("\nВведите ID задачи для удаления: ");

        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            Task task = findTaskById(id);

            if (task == null) {
                System.out.println("❌ Задача с ID " + id + " не найдена.");
                return;
            }

            System.out.println("\nВы уверены, что хотите удалить эту задачу?");
            System.out.println(task);
            System.out.print("\n(д/н): ");

            String confirm = scanner.nextLine().trim().toLowerCase();
            if (confirm.equals("д") || confirm.equals("да") || confirm.equals("y") || confirm.equals("yes")) {
                tasks.remove(task);
                System.out.println("✅ Задача успешно удалена!");
            } else {
                System.out.println("✖️ Удаление отменено.");
            }

        } catch (NumberFormatException e) {
            System.out.println("❌ Неверный формат ID.");
        }
    }

    // ==================== ФИЛЬТРАЦИЯ И ПОИСК ====================

    private void filterTasks() {
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("                ФИЛЬТРАЦИЯ ЗАДАЧ");
        System.out.println("═══════════════════════════════════════════════");

        System.out.println("Фильтровать по:");
        System.out.println("1. 🏷️  Статусу");
        System.out.println("2. ⚡ Приоритету");
        System.out.println("3. ⏰ Сроку выполнения");
        System.out.println("4. 📅 Просроченные задачи");
        System.out.println("5. 🔄 Недавно обновленные");
        System.out.println("0. ↩️  Назад");
        System.out.print("Выберите: ");

        String choice = scanner.nextLine().trim();
        List<Task> filteredTasks = new ArrayList<>();

        switch (choice) {
            case "1" -> {
                Status status = selectStatus();
                for (Task task : tasks) {
                    if (task.getStatus() == status) {
                        filteredTasks.add(task);
                    }
                }
                System.out.println("\n📋 Задачи со статусом: " + status);
            }
            case "2" -> {
                Priority priority = selectPriority();
                for (Task task : tasks) {
                    if (task.getPriority() == priority) {
                        filteredTasks.add(task);
                    }
                }
                System.out.println("\n📋 Задачи с приоритетом: " + priority);
            }
            case "3" -> {
                System.out.println("Срок выполнения:");
                System.out.println("1. 📅 На сегодня");
                System.out.println("2. ⏳ На этой неделе");
                System.out.println("3. 📆 В этом месяце");
                System.out.println("4. 🗓️  Без дедлайна");
                System.out.print("Выберите: ");

                String deadlineChoice = scanner.nextLine().trim();
                LocalDate now = LocalDate.now();

                switch (deadlineChoice) {
                    case "1" -> {
                        for (Task task : tasks) {
                            if (task.getDeadline() != null && task.getDeadline().equals(now)) {
                                filteredTasks.add(task);
                            }
                        }
                        System.out.println("\n📋 Задачи на сегодня");
                    }
                    case "2" -> {
                        LocalDate endOfWeek = now.plusDays(7);
                        for (Task task : tasks) {
                            if (task.getDeadline() != null &&
                                    !task.getDeadline().isBefore(now) &&
                                    !task.getDeadline().isAfter(endOfWeek)) {
                                filteredTasks.add(task);
                            }
                        }
                        System.out.println("\n📋 Задачи на этой неделе");
                    }
                    case "3" -> {
                        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
                        for (Task task : tasks) {
                            if (task.getDeadline() != null &&
                                    !task.getDeadline().isBefore(now) &&
                                    !task.getDeadline().isAfter(endOfMonth)) {
                                filteredTasks.add(task);
                            }
                        }
                        System.out.println("\n📋 Задачи в этом месяце");
                    }
                    case "4" -> {
                        for (Task task : tasks) {
                            if (task.getDeadline() == null) {
                                filteredTasks.add(task);
                            }
                        }
                        System.out.println("\n📋 Задачи без дедлайна");
                    }
                }
            }
            case "4" -> {
                for (Task task : tasks) {
                    if (task.isOverdue()) {
                        filteredTasks.add(task);
                    }
                }
                System.out.println("\n📋 Просроченные задачи");
            }
            case "5" -> {
                List<Task> sortedByUpdate = new ArrayList<>(tasks);
                sortedByUpdate.sort((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()));

                int limit = Math.min(10, sortedByUpdate.size());
                for (int i = 0; i < limit; i++) {
                    filteredTasks.add(sortedByUpdate.get(i));
                }
                System.out.println("\n📋 Недавно обновленные задачи");
            }
            case "0" -> { return; }
            default -> {
                System.out.println("❌ Неверный выбор.");
                return;
            }
        }

        if (filteredTasks.isEmpty()) {
            System.out.println("📭 Задачи не найдены.");
        } else {
            System.out.println("Найдено задач: " + filteredTasks.size() + "\n");
            for (Task task : filteredTasks) {
                System.out.println(task);
                System.out.println();
            }
        }
    }

    private void searchTasks() {
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("                  ПОИСК ЗАДАЧ");
        System.out.println("═══════════════════════════════════════════════");

        System.out.print("Введите текст для поиска (в названии или описании): ");
        String query = scanner.nextLine().trim().toLowerCase();

        if (query.isEmpty()) {
            System.out.println("❌ Введите текст для поиска.");
            return;
        }

        List<Task> foundTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getTitle().toLowerCase().contains(query) ||
                    task.getDescription().toLowerCase().contains(query)) {
                foundTasks.add(task);
            }
        }

        if (foundTasks.isEmpty()) {
            System.out.println("🔍 Задачи не найдены.");
        } else {
            System.out.println("\n🔍 Найдено задач: " + foundTasks.size() + "\n");
            for (Task task : foundTasks) {
                System.out.println(task);
                System.out.println();
            }
        }
    }

    // ==================== СТАТИСТИКА И ОТЧЕТЫ ====================

    private void showStatistics() {
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("                  СТАТИСТИКА");
        System.out.println("═══════════════════════════════════════════════");

        int totalTasks = tasks.size();
        int doneTasks = 0;
        int inProgressTasks = 0;
        int todoTasks = 0;
        int overdueTasks = 0;
        int highPriority = 0;
        int mediumPriority = 0;
        int lowPriority = 0;

        for (Task task : tasks) {
            switch (task.getStatus()) {
                case DONE -> doneTasks++;
                case IN_PROGRESS -> inProgressTasks++;
                case TODO -> todoTasks++;
            }

            if (task.isOverdue()) {
                overdueTasks++;
            }

            switch (task.getPriority()) {
                case HIGH -> highPriority++;
                case MEDIUM -> mediumPriority++;
                case LOW -> lowPriority++;
            }
        }

        System.out.printf("📊 Всего задач: %d\n", totalTasks);
        if (totalTasks > 0) {
            System.out.printf("✅ Выполнено: %d (%.1f%%)\n", doneTasks, doneTasks * 100.0 / totalTasks);
            System.out.printf("🔄 В процессе: %d (%.1f%%)\n", inProgressTasks, inProgressTasks * 100.0 / totalTasks);
            System.out.printf("📝 К выполнению: %d (%.1f%%)\n", todoTasks, todoTasks * 100.0 / totalTasks);
        } else {
            System.out.println("✅ Выполнено: 0 (0.0%)");
            System.out.println("🔄 В процессе: 0 (0.0%)");
            System.out.println("📝 К выполнению: 0 (0.0%)");
        }
        System.out.printf("⏰ Просрочено: %d\n", overdueTasks);

        System.out.println("\n⚡ Распределение по приоритетам:");
        System.out.printf("🔴 Высокий: %d задач\n", highPriority);
        System.out.printf("🟡 Средний: %d задач\n", mediumPriority);
        System.out.printf("🟢 Низкий: %d задач\n", lowPriority);

        // Самые старые невыполненные задачи
        List<Task> oldestTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getStatus() != Status.DONE) {
                oldestTasks.add(task);
            }
        }
        oldestTasks.sort(Comparator.comparing(Task::getCreatedAt));

        int limit = Math.min(3, oldestTasks.size());
        if (limit > 0) {
            System.out.println("\n📅 Самые старые невыполненные задачи:");
            for (int i = 0; i < limit; i++) {
                Task task = oldestTasks.get(i);
                System.out.printf("• ID %d: %s (создано: %s)\n",
                        task.getId(), task.getTitle(),
                        task.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            }
        }
    }

    private void showUpcomingTasks() {
        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("             ПРЕДСТОЯЩИЕ ЗАДАЧИ");
        System.out.println("═══════════════════════════════════════════════");

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);

        List<Task> upcomingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getDeadline() != null &&
                    !task.getDeadline().isBefore(today) &&
                    !task.getDeadline().isAfter(nextWeek) &&
                    task.getStatus() != Status.DONE &&
                    task.getStatus() != Status.CANCELLED) {
                upcomingTasks.add(task);
            }
        }

        if (upcomingTasks.isEmpty()) {
            System.out.println("🎉 Нет предстоящих задач на ближайшую неделю!");
        } else {
            System.out.println("📅 Задачи на ближайшую неделю (" + upcomingTasks.size() + "):\n");

            // Сортировка по дате
            upcomingTasks.sort(Comparator.comparing(Task::getDeadline));

            // Группируем по дням
            Map<LocalDate, List<Task>> tasksByDay = new TreeMap<>();
            for (Task task : upcomingTasks) {
                LocalDate deadline = task.getDeadline();
                tasksByDay.computeIfAbsent(deadline, k -> new ArrayList<>()).add(task);
            }

            for (Map.Entry<LocalDate, List<Task>> entry : tasksByDay.entrySet()) {
                System.out.println("📅 " + entry.getKey().format(DATE_FORMATTER) + ":");
                for (Task task : entry.getValue()) {
                    System.out.printf("   • [ID %d] %s (%s, %s)\n",
                            task.getId(), task.getTitle(), task.getPriority(), task.getStatus());
                }
                System.out.println();
            }
        }
    }

    // ==================== СОХРАНЕНИЕ И ЗАГРУЗКА ====================

    private void saveToCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
            writer.println("ID;Название;Описание;Статус;Приоритет;Дедлайн;Создано;Обновлено");

            for (Task task : tasks) {
                writer.println(task.toCSV());
            }

            System.out.println("✅ Данные сохранены в файл: " + CSV_FILE);
            System.out.println("📊 Сохранено задач: " + tasks.size());

        } catch (IOException e) {
            System.out.println("❌ Ошибка при сохранении в CSV: " + e.getMessage());
        }
    }

    private void loadFromCSV() {
        File file = new File(CSV_FILE);
        if (!file.exists()) {
            System.out.println("📂 Файл " + CSV_FILE + " не найден. Будет создан новый.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            List<Task> loadedTasks = new ArrayList<>();
            String line;
            boolean isFirstLine = true;
            int maxId = 0;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(";", -1);
                if (parts.length >= 8) {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        String title = parts[1];
                        String description = parts[2];
                        Status status = Status.valueOf(parts[3]);
                        Priority priority = Priority.valueOf(parts[4]);

                        LocalDate deadline = null;
                        if (!parts[5].isEmpty()) {
                            deadline = LocalDate.parse(parts[5]);
                        }

                        LocalDateTime createdAt = LocalDateTime.parse(parts[6]);
                        LocalDateTime updatedAt = LocalDateTime.parse(parts[7]);

                        Task task = new Task(id, title, description, status, priority, deadline, createdAt, updatedAt);
                        loadedTasks.add(task);
                        maxId = Math.max(maxId, id);

                    } catch (Exception e) {
                        System.out.println("⚠️  Ошибка при чтении строки: " + line);
                        System.out.println("    Причина: " + e.getMessage());
                    }
                }
            }

            tasks = loadedTasks;
            nextId = maxId + 1;

            System.out.println("✅ Данные загружены из файла: " + CSV_FILE);
            System.out.println("📊 Загружено задач: " + tasks.size());

        } catch (IOException e) {
            System.out.println("❌ Ошибка при загрузке из CSV: " + e.getMessage());
        }
    }

    private void saveToJSON() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(JSON_FILE))) {
            writer.println("[");

            for (int i = 0; i < tasks.size(); i++) {
                writer.print(tasks.get(i).toJSON());
                if (i < tasks.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }

            writer.println("]");

            System.out.println("✅ Данные сохранены в файл: " + JSON_FILE);
            System.out.println("📊 Сохранено задач: " + tasks.size());

        } catch (IOException e) {
            System.out.println("❌ Ошибка при сохранении в JSON: " + e.getMessage());
        }
    }

    private void loadFromJSON() {
        File file = new File(JSON_FILE);
        if (!file.exists()) {
            System.out.println("📂 Файл " + JSON_FILE + " не найден.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(JSON_FILE))) {
            List<Task> loadedTasks = new ArrayList<>();
            StringBuilder jsonContent = new StringBuilder();
            String line;
            int maxId = 0;

            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            String content = jsonContent.toString();
            content = content.replace("[", "").replace("]", "").trim();

            if (content.isEmpty()) {
                System.out.println("✅ Файл JSON пуст.");
                return;
            }

            // Разделяем объекты
            String[] objects = content.split("\\},\\{");

            for (int i = 0; i < objects.length; i++) {
                String obj = objects[i];
                if (i == 0) obj = obj.substring(1); // Убираем первую {
                if (i == objects.length - 1) obj = obj.substring(0, obj.length() - 1); // Убираем последнюю }

                Map<String, String> fields = parseJsonObject(obj);

                if (!fields.isEmpty()) {
                    try {
                        int id = Integer.parseInt(fields.get("id"));
                        String title = fields.get("title");
                        String description = fields.get("description");
                        Status status = Status.valueOf(fields.get("status"));
                        Priority priority = Priority.valueOf(fields.get("priority"));

                        LocalDate deadline = null;
                        String deadlineStr = fields.get("deadline");
                        if (deadlineStr != null && !deadlineStr.isEmpty() && !deadlineStr.equals("null")) {
                            deadline = LocalDate.parse(deadlineStr);
                        }

                        LocalDateTime createdAt = LocalDateTime.parse(fields.get("createdAt"));
                        LocalDateTime updatedAt = LocalDateTime.parse(fields.get("updatedAt"));

                        Task task = new Task(id, title, description, status, priority, deadline, createdAt, updatedAt);
                        loadedTasks.add(task);
                        maxId = Math.max(maxId, id);

                    } catch (Exception e) {
                        System.out.println("⚠️  Ошибка при парсинге объекта JSON");
                    }
                }
            }

            tasks = loadedTasks;
            nextId = maxId + 1;

            System.out.println("✅ Данные загружены из файла: " + JSON_FILE);
            System.out.println("📊 Загружено задач: " + tasks.size());

        } catch (IOException e) {
            System.out.println("❌ Ошибка при загрузке из JSON: " + e.getMessage());
        }
    }

    private Map<String, String> parseJsonObject(String json) {
        Map<String, String> fields = new HashMap<>();

        try {
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "").trim();
                    String value = keyValue[1].trim();

                    // Убираем кавычки если они есть
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    fields.put(key, value);
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка парсинга JSON: " + e.getMessage());
        }

        return fields;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Task findTaskById(int id) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                return task;
            }
        }
        return null;
    }

    private Status selectStatus() {
        while (true) {
            System.out.println("\nВыберите статус задачи:");
            Status[] statuses = Status.values();
            for (int i = 0; i < statuses.length; i++) {
                System.out.printf("%d. %s\n", i + 1, statuses[i]);
            }
            System.out.print("Ваш выбор: ");

            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= statuses.length) {
                    return statuses[choice - 1];
                }
            } catch (NumberFormatException e) {
                // Продолжаем цикл
            }
            System.out.println("❌ Неверный выбор. Попробуйте снова.");
        }
    }

    private Priority selectPriority() {
        while (true) {
            System.out.println("\nВыберите приоритет задачи:");
            Priority[] priorities = Priority.values();
            for (int i = 0; i < priorities.length; i++) {
                System.out.printf("%d. %s\n", i + 1, priorities[i]);
            }
            System.out.print("Ваш выбор: ");

            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= priorities.length) {
                    return priorities[choice - 1];
                }
            } catch (NumberFormatException e) {
                // Продолжаем цикл
            }
            System.out.println("❌ Неверный выбор. Попробуйте снова.");
        }
    }

    private LocalDate selectDeadline() {
        while (true) {
            System.out.println("\nУстановить дедлайн?");
            System.out.println("1. 📅 Установить дату");
            System.out.println("2. 🚫 Без дедлайна");
            System.out.print("Ваш выбор: ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("2")) {
                return null;
            } else if (choice.equals("1")) {
                try {
                    System.out.print("Введите дату в формате ДД.ММ.ГГГГ (например, 25.12.2024): ");
                    String dateStr = scanner.nextLine().trim();

                    if (dateStr.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

                        if (date.isBefore(LocalDate.now())) {
                            System.out.println("⚠️  Внимание: указанная дата уже прошла!");
                            System.out.print("Все равно установить? (д/н): ");
                            String confirm = scanner.nextLine().trim().toLowerCase();
                            if (!confirm.equals("д") && !confirm.equals("да")) {
                                continue;
                            }
                        }

                        return date;
                    } else {
                        System.out.println("❌ Неверный формат даты.");
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("❌ Неверная дата. Попробуйте снова.");
                }
            } else {
                System.out.println("❌ Неверный выбор.");
            }
        }
    }

    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    // ==================== ТОЧКА ВХОДА ====================

    public static void main(String[] args) {
        TaskPlanner planner = new TaskPlanner();
        planner.run();
    }
}
