package uga.lig.csp_clean_layerCellsWactions;

public class ValidatorItem {
    private int cell;
    private int layer;
    private int id;
    private boolean isMethod;

    public ValidatorItem(int cellVal, int layerVal, int idVal, boolean methodVal) {
        this.cell = cellVal;
        this.layer = layerVal;
        this.id = idVal;
        this.isMethod = methodVal;
    }

    public int getLayer() {
        return this.layer;
    }

    public int getCell() {
        return this.cell;
    }

    public void setLayer(int a) {
        this.layer = a;
    }

    public void setCell(int a) {
        this.cell = a;
    }

    public int getId() {
        return this.id;
    }

    public boolean getIsMethod() {
        return this.isMethod;
    }

}
