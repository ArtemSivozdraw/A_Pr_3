import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class First {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Scanner sc = new Scanner(System.in);

        System.out.print("Введіть розмір матриці : ");
        int size = sc.nextInt();
        System.out.print("Введіть максимальне значення елементу масива : ");
        int max = sc.nextInt();
        System.out.print("Введіть мінімальне значення елементу масива : ");
        int min = sc.nextInt();

        int[][] matrix = generate_matrix(size,max,min);
        print_matrix(matrix);

        long start_time = System.nanoTime();                                        // Початок секції Work_Dealing

        int number_of_thread = 8;
        ExecutorService es = Executors.newFixedThreadPool(number_of_thread);               // Створення пулу потоків
        List<Future<List<Integer>>> futures = new ArrayList<>();                            // Створення списку Futures для зберігання виконання задачі

        int interval_length = (int) Math.ceil((double)size / number_of_thread);
        for (int i=0;i<number_of_thread; i++){                                              // Розбиття матриці на окремі рівні частини
            int start = i*interval_length;
            int end = Math.min(start+interval_length,size);
            Callable<List<Integer>> task = new UnrealCallable(matrix,start,end);            // Створення задач та передача їм їхні частини роботи
            futures.add(es.submit(task));                                                   // Запуск виконання задач
        }

        List<Integer> final_result_work_dealer = new CopyOnWriteArrayList<>();              // Зміна де зберігається фінальний результат
        for(Future<List<Integer>> future : futures){
            final_result_work_dealer.addAll(future.get());                                  // Збирання результатів виконання кожних підзадач
        }

        if(final_result_work_dealer.isEmpty()){
            System.out.println("У масиві немає таких чисел");
        } else{
            System.out.println(final_result_work_dealer);
        }
        es.shutdown();                                                      // Кінець роботи секції Work_Dealing

        long end_time = System.nanoTime();
        System.out.println("Час виконання - "+(end_time-start_time)/1000000 + " mls");


        start_time = System.nanoTime();                                     // Початок роботи секції work_stealing

        ForkJoinPool pool = new ForkJoinPool();                             // Створення пулу потоків ForkJoin
        List<Future<List<Integer>>> futures1 = new ArrayList<>();           // Створення списку Future для зберігання виконання завдання
        for(int i=0;i<matrix.length;i++){
            Work_Stealing task = new Work_Stealing(matrix[i],0,matrix.length,i);            // Створення нових задач
            futures1.add(pool.submit(task));                                                     // Запуск задач
        }

        List<Integer> final_result_work_stealer = new CopyOnWriteArrayList<>();                 // Список для зберігання результату
        for(Future<List<Integer>> future : futures1){
            final_result_work_stealer.addAll(future.get());                                     // Збирання результатів всіх підзадач
        }

        if(final_result_work_stealer.isEmpty()){
            System.out.println("У масиві немає таких чисел");
        } else{
            System.out.println(final_result_work_dealer);
        }
        pool.shutdown();

        end_time = System.nanoTime();                                                           // Кінеьц роботи секції Work_stealing
        System.out.println("Час виконання - "+(end_time-start_time)/1000000 + " mls");

    }

    public static int[][] generate_matrix(int size, int max, int min){          // Функція для створення амтриці
        int[][] result = new int[size][size];
        for(int i=0; i<size; i++){
            for(int j=0;j<size;j++){
                result[i][j] = min +  (int) (Math.random()*(max-min));
            }
        }
        return result;
    }
    public static void print_matrix(int[][] matrix){                    // Функція для виводу матриці
        for(int i=0;i<matrix.length;i++){
            for(int j=0;j< matrix.length;j++){
                System.out.printf(matrix[i][j]+" ");
            }
            System.out.println();
        }
    }

}
class Work_Stealing extends RecursiveTask<List<Integer>> {            // Клас для ForkJoin
    int[] array;
    int start;
    int end;
    int row_index;

    public Work_Stealing(int [] array, int start, int end, int row_index){
        this.array = array;
        this.start = start;
        this.end = end;
        this.row_index = row_index;
    }

    @Override
    protected List<Integer> compute() {
        List<Integer> result = new ArrayList<>();               // Змінна де зберігається результат роботи потоку
        if(end-start > array.length/2){                         // кожен рядок що передається сюди ділиться попалам. Я доволі довго експерементував з цією перевіркою і це накращий варіант, тому що при частішому поділу рядка може викликаьтись помилка StackOverFlow
            int mid = (end-start)/2;

            Work_Stealing left_task = new Work_Stealing(array,start,mid,row_index);         // В самій задачі створюються дві підзадачі
            Work_Stealing right_task = new Work_Stealing(array,mid,end,row_index);          // Це зроблено для того щоб, потоки що бистріше справились
                                                                                            // могли красти роботу одне у одного
            left_task.fork();               // Запуск обох частин
            right_task.fork();

            List<Integer> left_result = left_task.join();           // Отримання результату
            List<Integer> right_result = right_task.join();

            result.addAll(left_result);                     // Занесення його до глобальної зміної
            result.addAll(right_result);

        }else {
            for (int i = start; i < end; i++) {             // Якщо рядок уже потріблено достатньо, його обробляють, а не ділять знову
                if (array[i] == i + row_index) {
                    result.add(array[i]);
                }
            }
        }

        return result;
    }
}

class UnrealCallable implements Callable<List<Integer>>{            // клас для ExecutorService
    int[][] matrix;
    int start;
    int finish;
    public UnrealCallable(int[][] matrix, int start, int finish){
        this.matrix = matrix;
        this.start = start;
        this.finish = finish;
    }
    @Override
    public List<Integer> call() throws Exception {                  // Спокійне тихе проходження всіх елементів даного масива і перевірка (Aij == I+J)
        List<Integer> result = new CopyOnWriteArrayList<>();
        for(int j=0; j< matrix.length; j++) {
            for (int i = start; i < finish; i++) {
                if (matrix[j][i] == i + j) {
                    result.add(matrix[j][i]);
                }
            }
        }
        return result;
    }
}
