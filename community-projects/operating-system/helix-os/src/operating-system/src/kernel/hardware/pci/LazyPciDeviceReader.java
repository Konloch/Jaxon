package kernel.hardware.pci;

public class LazyPciDeviceReader extends PCI {
    private int _currentBus;
    private int _currentDevice;
    private int _currentFunction;
    private boolean _isFinished = false;

    public LazyPciDeviceReader() {
        _currentBus = 0;
        _currentDevice = 0;
        _currentFunction = 0;
    }

    public PciDevice Next() {
        PciDevice device = Read(_currentBus, _currentDevice, _currentFunction);
        _currentFunction++;
        if (_currentFunction >= MAX_FUNCTIONS) {
            _currentFunction = 0;
            _currentDevice++;
            if (_currentDevice >= MAX_DEVICES) {
                _currentDevice = 0;
                _currentBus++;
                if (_currentBus >= MAX_BUS) {
                    _currentBus = MAX_BUS;
                    _isFinished = true;
                }
            }
        }

        return device;
    }

    @SJC.Inline
    public boolean HasNext() {
        return !_isFinished;
    }

    public void Reset() {
        _currentBus = 0;
        _currentDevice = 0;
        _currentFunction = 0;
        _isFinished = false;
    }
}
