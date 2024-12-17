import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Temp {
    // Глобальний список для збереження шляхів до фото
    private static final List<File> photoList = new ArrayList<>();

    // Клас для рекурсивного обходу директорій
    static class DirectoryTask extends RecursiveTask<Void> {
        private final File directory;

        public DirectoryTask(File directory) {
            this.directory = directory;
        }

        @Override
        protected Void compute() {
            List<DirectoryTask> subTasks = new ArrayList<>();

            // Перевіряємо, чи це директорія
            if (directory.isDirectory()) {
                System.out.println("Обробляємо директорію: " + directory.getAbsolutePath());

                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            // Якщо це папка, створюємо нову задачу
                            DirectoryTask task = new DirectoryTask(file);
                            task.fork(); // Запускаємо задачу
                            subTasks.add(task);
                        } else {
                            // Перевіряємо, чи це фото
                            if (isPhoto(file)) {
                                synchronized (photoList) {
                                    photoList.add(file);
                                }
                                System.out.println("Знайдено фото: " + file.getAbsolutePath());
                            }
                        }
                    }
                }
            }

            // Об'єднуємо всі підзадачі
            for (DirectoryTask task : subTasks) {
                task.join();
            }

            return null;
        }

        // Метод для перевірки, чи є файл фото
        private boolean isPhoto(File file) {
            String fileName = file.getName().toLowerCase();
            return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
        }
    }

    public static void main(String[] args) {
        String rootDirectory = "D:\\"; // Коренева директорія

        // Створюємо ForkJoinPool
        ForkJoinPool pool = new ForkJoinPool();

        // Запускаємо основну задачу
        DirectoryTask rootTask = new DirectoryTask(new File(rootDirectory));
        pool.invoke(rootTask);

        // Закриваємо пул потоків
        pool.shutdown();

        // Відкриваємо останнє фото
        if (!photoList.isEmpty()) {
            File lastPhoto = photoList.get(photoList.size() - 1);
            System.out.println("\nВідкриваємо останнє фото: " + lastPhoto.getAbsolutePath());
            openPhoto(lastPhoto);
        } else {
            System.out.println("Фото не знайдено.");
        }
    }

    // Метод для відкриття фото за допомогою стандартного переглядача
    private static void openPhoto(File photo) {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(photo);
            } else {
                System.out.println("Відкриття файлів не підтримується на цьому комп'ютері.");
            }
        } catch (IOException e) {
            System.out.println("Не вдалося відкрити фото: " + e.getMessage());
        }
    }
}
