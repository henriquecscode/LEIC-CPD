import java.util.Arrays;

public class MatrixMultiplication{

    public static void main(String[] args) {
        int n = 1000;
        int a[][] = new int[n][n];
        int b[][] = new int[n][n];
        int c[][] = new int[n][n];

        for(int[] row: a){
            Arrays.fill(row, 1);
        }
        for (int i = 0; i < n; i++) {
            Arrays.fill(b[i], i+1);
        }
        for(int[] row: c){
            Arrays.fill(row, 0);
        }


        multiplication(a, b, c, n);
    }

    public static void multiplication(int[][] a, int[][] b, int[][] c, int n) {

        long startTime = System.nanoTime();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    c[i][j] = c[i][j] + a[i][k] * b[k][j];
                }
            }
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        for (int i = 0; i < 10; i++) {
            System.out.print(c[0][i]);
            System.out.print(" ");
        }
        System.out.println("");
        System.out.println("Time " + duration);
    }
}
