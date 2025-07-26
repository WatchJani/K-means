import mpi.MPI;
import mpi.MPIException;

public class MPIMain {
    public static void main(String[] args) throws MPIException {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        System.out.println("Hello from process " + rank + " of " + size);

        MPI.Finalize();
    }
}