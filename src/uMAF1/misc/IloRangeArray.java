package uMAF1.misc;


import ilog.concert.IloRange;

public class IloRangeArray {

    int _num           = 0;
    IloRange[] _array = new IloRange[60];

    public void add(IloRange ivar) {
        if ( _num >= _array.length ) {
            IloRange[] array = new IloRange[2 * _array.length];
            System.arraycopy(_array, 0, array, 0, _num);
            _array = array;
        }
        _array[_num++] = ivar;
    }

    IloRange getElement(int i) {
        return _array[i];
    }

    int getSize() {
        return _num;
    }

    IloRange[] getArray() {
        IloRange[] array = new IloRange[_num];
        for (int i = 0; i < _num; i++) {
            array[i] = _array[i];
        }
        return array;
    }
}

