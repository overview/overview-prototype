package snappy.pack;

public class CanvasCell {
    public boolean occupied;

    public CanvasCell(boolean occupied) { this.occupied = occupied; }
    public CanvasCell() { this.occupied = false; }

    public String toString() { return occupied ? "x" : "."; }

}
