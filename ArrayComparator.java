import java.util.Comparator;

// good for sorting the populations on the 2nd index...
class ArrayComparator implements Comparator<double[]> 
{
    private final int columnToSort;
    private final boolean ascending;

    public ArrayComparator(int columnToSort, boolean ascending) {
        this.columnToSort = columnToSort;
        this.ascending = ascending;
    }

    public int compare(double[] c1, double[] c2) {
        int cmp = (c1[columnToSort] > c2[columnToSort]) ? 1 : -1;
        cmp = (c1[columnToSort] == c2[columnToSort]) ? 0 : cmp;
        return ascending ? cmp : -cmp;
    }
}