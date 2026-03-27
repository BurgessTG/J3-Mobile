import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            Color.black
                .ignoresSafeArea()
            ComposeView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
