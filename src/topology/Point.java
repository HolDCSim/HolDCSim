package topology;

public class Point {
    private int x;
    private int y;
    private int value;
 
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
 
    public Point(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }
 
    public int getX() {
        return x;
    }
 
    public void setX(int x) {
        this.x = x;
    }
 
    public int getY() {
        return y;
    }
 
    public void setY(int y) {
        this.y = y;
    }
 
    public int getValue() {
        return value;
    }
 
    public void setValue(int value) {
        this.value = value;
    }
 
    @Override
    public String toString() {
        return "{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
 
        Point point = (Point) o;
 
        if (x != point.x) return false;
        return y == point.y;
    }
 
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}

