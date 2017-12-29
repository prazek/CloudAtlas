import grpc
import agent_pb2
import agent_pb2_grpc

def run():
  channel = grpc.insecure_channel('localhost:4321')
  msg = agent_pb2.PathName(p="xxx")
  print(msg)
  stub = agent_pb2_grpc.AgentStub(channel)
  print(stub.GetZone(msg))

  print(channel)

if __name__ == '__main__':
  run()