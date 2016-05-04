package mod.steamnsteel.client.model.common;

/**
 * Created by steblo on 23/03/2016.
 */
public class Pivot implements IKind<Pivot> {
    private Node<Pivot> parent;

    @Override
    public void setParent(Node<Pivot> parent) {
        this.parent = parent;
    }

    @Override
    public Node<Pivot> getParent() {
        return parent;
    }
}
