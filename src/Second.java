import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Second {


    static class DirectoryTask extends RecursiveTask<List<File>> {
        private final File directory;

        public DirectoryTask(File directory) {      // Передається лише шлях кореневої папки
            this.directory = directory;
        }

        @Override
        protected List<File> compute() {
            List<File> photoList = new ArrayList<>();                   // Список, де будуть зберігатися зображення
            List<DirectoryTask> subTasks = new ArrayList<>();           // Список, де будуть зберігатися створені підзадачі


            if (directory.isDirectory()) {
                File[] files = directory.listFiles();                   // Масив файлів кореневої папки
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {

                            DirectoryTask task = new DirectoryTask(file);   // якщо в кореневій папці виявлено ще одну папку, нова папка передається новій задачі як коренева
                            task.fork();                                    // запуск задачі в пул
                            subTasks.add(task);                             // Збереження задачі, для того щоб в майбутньому витягти з неї результат
                        } else {

                            if (isPhoto(file)) {                            // якщо це фото
                                photoList.add(file);                        // то добавити до списку фотографій
                                System.out.println(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }


            for (DirectoryTask task : subTasks) {
                photoList.addAll(task.join());                         // Збір всіх підзадач
            }

            return photoList;
        }


        private boolean isPhoto(File file) {                        // Функція яка перевіряє чи є файл фотографією
            String fileName = file.getName().toLowerCase();
            return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
        }
    }


    public static List<File> scan(String rootDirectory) {
        ForkJoinPool pool = new ForkJoinPool();                                     // Створення пулу ForkJoin
        DirectoryTask rootTask = new DirectoryTask(new File(rootDirectory));        // Створення задачі
        List<File> photoList = pool.invoke(rootTask);                               // Запуск та збереження резульатту виконання задачі до списка
        pool.shutdown();
        return photoList;
    }


    public static void openPhoto(File photo) {                  //Функція яка відкриває фотографію
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

    public static void main(String[] args) {
        String rootDirectory = "C:\\";


        List<File> photoList = Second.scan(rootDirectory); // Запуск проходження директорій


        if (!photoList.isEmpty()) {
            System.out.println("\nЗнайдені фото.");

            // Відкриваємо останнє фото
            File lastPhoto = photoList.get(photoList.size() - 1);
            System.out.println("\nВідкриваємо останнє фото: " + lastPhoto.getAbsolutePath());
            Second.openPhoto(lastPhoto);
        } else {
            System.out.println("Фото не знайдено.");
        }
    }
}
