package uMAF1.misc;

import ilog.concert.IloNumVar;

public class IloNumVarArray {
    public int _num = 0;
    IloNumVar[] _array = new IloNumVar[60];

    public void add(IloNumVar ivar) {
        if ( _num >= _array.length ) {
            IloNumVar[] array = new IloNumVar[2 * _array.length];
            System.arraycopy(_array, 0, array, 0, _num);
            _array = array;
        }
        _array[_num++] = ivar;
    }

    public IloNumVar getElement(int i) {
        return _array[i];
    }
    int getSize() {
        return _num;
    }
    public IloNumVar[] getArray(){
        IloNumVar[] array = new IloNumVar[_num];
        for(int i=0;i<_num;i++){
            array[i]=_array[i];
        }
        return array;
    }
}
