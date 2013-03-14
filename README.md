examples
========
Forking Martin's code to compare off-heap implementation of single producer/consumer
Q with original. Taking the off heap implementation and using it to implement and IPC
queue.
P1C1OffHeapQueue is the end result for an integer IPC queue. To run the perf test
launch a producer process(first) and a consumer process:
java -cp bin psy.lob.saw.ipc.IpcPerfTest p
java -cp bin psy.lob.saw.ipc.IpcPerfTest c