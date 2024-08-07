import java.util.Arrays;

public class MatrixBlockMultiplication {
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

    // https://stackoverflow.com/questions/16115770/block-matrix-multiplication
    public static void multiplication(int[][] a, int[][] b, int[][] c, int n) {
    int cachesizeKB = 384; // Changes according to specs
    int datasize = 4; // 4 bytes for int
    int block = (int) Math.floor(Math.sqrt(cachesizeKB/3/datasize));
        long startTime = System.nanoTime();
        for(int jj=0;jj<n;jj+= block){
            for(int kk=0;kk<n;kk+= block){
                for(int i=0;i<n;i++){
                    for(int j = jj; j<((jj+block)>n?n:(jj+block)); j++){
                        int temp = 0;
                        for(int k = kk; k<((kk+block)>n?n:(kk+block)); k++){
                            temp += a[i][k]*b[k][j];
                        }
                        c[i][j] += temp;
                    }
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
